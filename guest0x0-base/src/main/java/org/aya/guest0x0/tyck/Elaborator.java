package org.aya.guest0x0.tyck;

import kala.collection.SeqView;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.syntax.*;
import org.aya.guest0x0.util.SPE;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record Elaborator(
  @NotNull MutableMap<LocalVar, Def<Term>> sigma,
  @NotNull MutableMap<LocalVar, Term> gamma
) {
  @NotNull public Term normalize(@NotNull Term term) {
    return new Normalizer(sigma, MutableMap.create()).term(term);
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Term type) {}

  public Term inherit(Expr expr, Term type) {
    return switch (expr) {
      case Expr.Lam lam -> switch (normalize(type)) {
        case Term.DT dt && dt.isPi() -> {
          var body = hof(lam.x(), dt.param().type(), () -> inherit(lam.a(), dt.codomain(new Term.Ref(lam.x()))));
          yield new Term.Lam(new Param<>(lam.x(), dt.param().type()), body);
        }
        // Overloaded lambda
        case Term.Path path -> {
          var tyDims = path.data().dims();
          var lamDims = MutableArrayList.<LocalVar>create(tyDims.size());
          var unlam = Expr.unlam(lamDims, tyDims.size(), lam);
          if (unlam == null) throw new SPE(lam.pos(), Doc.english("Expected (path) lambda"));
          yield boundaries(lamDims, () -> inherit(unlam,
            new Normalizer(sigma, MutableMap.from(
              lamDims.zipView(tyDims).map(t -> Tuple.of(t._1, new Term.Ref(t._2)))
            )).term(path.data().type()) // The expected type if the lambda body
          ), unlam.pos(), path.data());
        }
        default -> throw new SPE(lam.pos(),
          Doc.english("Expects a right adjoint for"), expr, Doc.plain("got"), type);
      };
      case Expr.Two two && !two.isApp() -> {
        if (!(normalize(type) instanceof Term.DT dt) || dt.isPi()) throw new SPE(two.pos(),
          Doc.english("Expects a left adjoint for"), expr, Doc.plain("got"), type);
        var lhs = inherit(two.f(), dt.param().type());
        yield new Term.Two(false, lhs, inherit(two.a(), dt.codomain(lhs)));
      }
      case Expr.Hole hole -> {
        var docs = MutableList.<Doc>create();
        gamma.forEach((k, v) -> {
          var list = MutableList.of(Doc.plain(k.name()), Doc.symbol(":"), normalize(v).toDoc());
          if (!hole.accessible().contains(k)) list.append(Doc.english("(hidden)"));
          docs.append(Doc.sep(list));
        });
        docs.append(Doc.plain("----------------------------------"));
        docs.append(type.toDoc());
        docs.append(Doc.symbol("|->"));
        docs.append(normalize(type).toDoc());
        throw new SPE(hole.pos(), Doc.vcat(docs));
      }
      default -> {
        var synth = synth(expr);
        yield switch (normalize(type)) {
          case Term.Path path -> {
            var exTyDims = path.data().dims();
            var lamDims = MutableArrayList.<LocalVar>create(exTyDims.size());
            var unlam = Term.unlam(lamDims, synth.wellTyped, exTyDims.size());
            var acTyDims = MutableArrayList.<LocalVar>create(exTyDims.size());
            var unpi = Term.unpi(acTyDims, synth.type, exTyDims.size());
            if (unlam == null || unpi == null) throw new SPE(expr.pos(), Doc.english("Expected (path) lambda"));
            unify(unpi, unlam, new Normalizer(sigma, MutableMap.from(
              exTyDims.zipView(acTyDims).map(t -> Tuple.of(t._1, new Term.Ref(t._2)))
            )).term(path.data().type()), expr.pos());
            yield boundaries(lamDims, () -> unlam, expr.pos(), path.data());
          }
          case Term ty -> {
            unify(ty, synth.wellTyped, synth.type, expr.pos());
            yield synth.wellTyped;
          }
        };
      }
    };
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos) {
    if (!Unifier.untyped(actual, ty))
      throw new SPE(pos, Doc.plain("Expects type"), ty, Doc.plain("got"),
        actual, Doc.english("on"), on);
  }

  private @NotNull Term boundaries(
    @NotNull MutableList<LocalVar> lamDims,
    @NotNull Supplier<Term> coreSupplier,
    SourcePos pos, Boundary.Data<Term> data
  ) {
    lamDims.forEach(t -> gamma.put(t, Term.I));
    var core = coreSupplier.get();
    for (var boundary : data.boundaries()) {
      var jon = jonSterling(lamDims.view(), boundary).term(core);
      if (!Unifier.untyped(boundary.body(), jon)) throw new SPE(pos,
        Doc.english("Boundary mismatch, expect"), boundary.body(), Doc.plain("got"), jon);
    }
    lamDims.forEach(gamma::remove);
    return new Term.PLam(lamDims.toImmutableArray(), core);
  }

  public Synth synth(Expr expr) {
    var synth = switch (expr) {
      case Expr.UI u -> new Synth(new Term.UI(u.isU()), Term.U);
      case Expr.End lr -> new Synth(new Term.End(lr.isLeft()), Term.I);
      case Expr.Resolved resolved -> {
        var type = gamma.getOrNull(resolved.ref());
        if (type != null) yield new Synth(new Term.Ref(resolved.ref()), type);
        var def = sigma.get(resolved.ref());
        var pi = Term.mkPi(def.telescope(), def.result());
        yield switch (def) {
          case Def.Fn<Term> fn -> new Synth(Normalizer.rename(Term.mkLam(fn.telescope(), fn.body())), pi);
        };
      }
      case Expr.Proj proj -> {
        var t = synth(proj.t());
        if (!(t.type instanceof Term.DT dt) || dt.isPi())
          throw new SPE(proj.pos(), Doc.english("Expects a left adjoint, got"), t.type);
        var fst = new Term.Proj(t.wellTyped, true);
        if (proj.isOne()) yield new Synth(fst, dt.param().type());
        yield new Synth(new Term.Proj(t.wellTyped, false), dt.codomain(fst));
      }
      case Expr.Two two -> {
        var f = synth(two.f());
        if (two.isApp()) {
          if (!(f.type instanceof Term.DT dt) || !dt.isPi())
            throw new SPE(two.pos(), Doc.english("Expects pi, got"), f.type, Doc.plain("when checking"), two);
          var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
          yield new Synth(Term.mkApp(f.wellTyped, a), dt.codomain(a));
        } else {
          var a = synth(two.a());
          yield new Synth(new Term.Two(false, f.wellTyped, a.wellTyped),
            new Term.DT(false, new Param<>(new LocalVar("_"), f.type), a.type));
        }
      }
      case Expr.DT dt -> {
        var param = synth(dt.param().type());
        var x = dt.param().x();
        var cod = hof(x, param.wellTyped, () -> synth(dt.cod()));
        yield new Synth(new Term.DT(dt.isPi(), new Param<>(x, param.wellTyped), cod.wellTyped), cod.type);
      }
      case Expr.Path path -> {
        var dims = path.data().dims();
        for (var dim : dims) gamma.put(dim, Term.I);
        var ty = inherit(path.data().type(), new Term.UI(true));
        var boundaries = MutableArrayList.<Boundary<Term>>create(path.data().boundaries().size());
        for (var boundary : path.data().boundaries()) {
          if (!dims.sizeEquals(boundary.pats())) throw new SPE(path.pos(),
            Doc.english("Expects " + dims.size() + " patterns, got: " + boundary.pats().size()));
          var term = inherit(boundary.body(), jonSterling(dims.view(), boundary).term(ty));
          boundaries.append(new Boundary<>(boundary.pats(), term));
        }
        var data = new Boundary.Data<>(dims, ty, boundaries.toImmutableArray());
        yield new Synth(new Term.Path(data), Term.U);
      }
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    if (type instanceof Term.Path path) {
      var binds = path.data().dims().map(x -> new Param<>(x, Term.I));
      return new Synth(Normalizer.rename(Term.mkLam(binds,
        new Term.PCall(synth.wellTyped, path.data().dims().map(Term.Ref::new), path.data()))),
        Term.mkPi(binds, path.data().type()));
    } else return new Synth(synth.wellTyped, type);
  }

  private @NotNull Normalizer jonSterling(SeqView<LocalVar> dims, Boundary<?> boundary) {
    return new Normalizer(sigma, MutableMap.from(dims
      .zip(boundary.pats()).filter(p -> p._2 != Boundary.Case.VAR)
      .map(p -> Tuple.of(p._1, new Term.End(p._2 == Boundary.Case.LEFT)))));
  }

  private <T> T hof(@NotNull LocalVar x, @NotNull Term type, @NotNull Supplier<T> t) {
    gamma.put(x, type);
    var ok = t.get();
    gamma.remove(x);
    return ok;
  }

  public Def<Term> def(Def<Expr> def) {
    return switch (def) {
      case Def.Fn<Expr> fn -> {
        var telescope = MutableArrayList.<Param<Term>>create(fn.telescope().size());
        for (var param : def.telescope()) {
          var ty = inherit(param.type(), Term.U);
          telescope.append(new Param<>(param.x(), ty));
          gamma.put(param.x(), ty);
        }
        var result = inherit(fn.result(), Term.U);
        var body = inherit(fn.body(), result);
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Fn<>(def.name(), telescope.toImmutableArray(), result, body);
      }
    };
  }
}
