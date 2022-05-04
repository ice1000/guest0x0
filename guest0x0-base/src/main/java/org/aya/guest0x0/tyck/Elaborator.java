package org.aya.guest0x0.tyck;

import kala.collection.SeqView;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.cubical.Boundary;
import org.aya.guest0x0.cubical.CofThy;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.BdryData;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.AltF7;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
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
    return normalizer().term(term);
  }

  private @NotNull Normalizer normalizer() {
    return new Normalizer(sigma, MutableMap.create());
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Term type) {}

  public Term inherit(Expr expr, Term type) {
    return switch (expr) {
      case Expr.Lam lam -> switch (normalize(type)) {
        case Term.DT dt && dt.isPi() -> new Term.Lam(lam.x(),
          hof(lam.x(), dt.param().type(), () -> inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())))));
        // Overloaded lambda
        case Term.Path path -> {
          var tyDims = path.data().dims();
          var lamDims = MutableArrayList.<LocalVar>create(tyDims.size());
          var unlam = Expr.unlam(lamDims, tyDims.size(), lam);
          if (unlam == null) throw new SPE(lam.pos(), Doc.english("Expected path lambda"));
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
          var list = MutableList.of(Doc.plain(k.name()));
          if (!hole.accessible().contains(k)) list.append(Doc.english("(out of scope)"));
          list.appendAll(new Doc[]{Doc.symbol(":"), normalize(v).toDoc()});
          docs.append(Doc.sep(list));
        });
        docs.append(Doc.plain("----------------------------------"));
        var tyDoc = type.toDoc();
        docs.append(tyDoc);
        var normDoc = normalize(type).toDoc();
        if (!tyDoc.equals(normDoc)) {
          docs.append(Doc.symbol("|->"));
          docs.append(normDoc);
        }
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
    var unifier = new Unifier();
    if (!unifier.untyped(actual, ty)) throw new SPE(pos, unifyDoc(ty, on, actual, unifier));
  }

  private @NotNull Doc unifyDoc(Term ty, Docile on, @NotNull Term actual, Unifier unifier) {
    var line1 = Doc.sep(Doc.plain("Expects"), ty.toDoc(), Doc.plain("got"),
      actual.toDoc(), Doc.english("on"), on.toDoc());
    if (unifier.data != null) {
      var line2 = Doc.sep(Doc.english("In particular,"),
        unifier.data.l().toDoc(), Doc.symbol("!="), unifier.data.r().toDoc());
      line1 = Doc.vcat(line1, line2);
    }
    return line1;
  }

  private @NotNull Term boundaries(
    @NotNull MutableList<LocalVar> lamDims,
    @NotNull Supplier<Term> coreSupplier,
    SourcePos pos, BdryData<Term> data
  ) {
    lamDims.forEach(t -> gamma.put(t, Term.I));
    var core = coreSupplier.get();
    for (var boundary : data.boundaries()) {
      var jon = jonSterling(lamDims.view(), boundary.face()).term(core);
      var unifier = new Unifier();
      if (!unifier.untyped(boundary.body(), jon)) throw new SPE(pos,
        Doc.vcat(Doc.english("Boundary mismatch, oh no."),
          unifyDoc(boundary.body(), boundary.face(), jon, unifier)));
    }
    lamDims.forEach(gamma::remove);
    return new Term.PLam(lamDims.toImmutableArray(), core);
  }

  public Synth synth(Expr expr) {
    var synth = switch (expr) {
      // TODO implement face type in core
      case Expr.PrimTy u -> new Synth(new Term.UI(u.keyword() == Expr.Keyword.U), Term.U);
      case Expr.Resolved resolved -> {
        var type = gamma.getOrNull(resolved.ref());
        if (type != null) yield new Synth(new Term.Ref(resolved.ref()), type);
        var def = sigma.get(resolved.ref());
        var pi = Term.mkPi(def.telescope(), def.result());
        yield switch (def) {
          case Def.Fn<Term> fn -> new Synth(Normalizer.rename(Term.mkLam(
            fn.telescope().view().map(Param::x), fn.body())), pi);
        };
      }
      case Expr.Proj proj -> {
        var t = synth(proj.t());
        if (!(t.type instanceof Term.DT dt) || dt.isPi())
          throw new SPE(proj.pos(), Doc.english("Expects a left adjoint, got"), t.type);
        var fst = t.wellTyped.proj(true);
        if (proj.isOne()) yield new Synth(fst, dt.param().type());
        yield new Synth(t.wellTyped.proj(false), dt.codomain(fst));
      }
      case Expr.Two two -> {
        var f = synth(two.f());
        if (two.isApp()) {
          if (!(f.type instanceof Term.DT dt) || !dt.isPi())
            throw new SPE(two.pos(), Doc.english("Expects pi, got"), f.type, Doc.plain("when checking"), two);
          var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
          yield new Synth(f.wellTyped.app(a), dt.codomain(a));
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
          if (!dims.sizeEquals(boundary.face().pats())) throw new SPE(path.pos(),
            Doc.english("Expects " + dims.size() + " patterns, got: " + boundary.face().pats().size()));
          var term = inherit(boundary.body(), jonSterling(dims.view(), boundary.face()).term(ty));
          boundaries.append(new Boundary<>(boundary.face(), term));
        }
        var data = new BdryData<>(dims, ty, boundaries.toImmutableArray());
        YouTrack.jesperCockx(data, path.pos());
        for (var dim : dims) gamma.remove(dim);
        yield new Synth(new Term.Path(data), Term.U);
      }
      case Expr.Mula f -> switch (f.asFormula()) {
        case Formula.Inv<Expr> inv -> new Synth(Term.neg(inherit(inv.i(), Term.I)), Term.I);
        case Formula.Conn<Expr> conn -> new Synth(new Term.Mula(
          new Formula.Conn<>(conn.isAnd(), inherit(conn.l(), Term.I), inherit(conn.r(), Term.I))), Term.I);
        case Formula.Lit lit -> new Synth(Term.end(lit.isLeft()), Term.I);
      };
      case Expr.Transp transp -> {
        var cover = inherit(transp.cover(), Term.mkPi(Term.I, Term.U));
        var detective = new AltF7(new LocalVar("?"));
        var sample = cover.app(new Term.Ref(detective.var()));
        var ty = Term.mkPi(cover.app(Term.end(true)), cover.app(Term.end(false)));
        var psi = transp.restr().mapCond(c -> new Restr.Cond<>(inherit(c.inst(), Term.I), c.isLeft()));
        // I believe find-usages is slightly more efficient than what Huber wrotes in hcomp.pdf
        var capture = new Object() {
          Term under = sample;
        };
        // I want it to have no references, so !detective.press(), and if it returns true, it means no problem
        // So if it returns false then we're in trouble
        if (!CofThy.vdash(psi, normalizer(), n -> !detective.press(capture.under = n.term(sample))))
          throw new SPE(transp.pos(), Doc.english("The cover"), cover,
            Doc.english("has to be constant under the cofibration"), psi,
            Doc.english("but applying a variable `?` to it results in"), capture.under,
            Doc.english("which contains a reference to `?`, oh no"));
        yield new Synth(new Term.Transp(cover, psi), ty);
      }
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    if (type instanceof Term.Path path) {
      var dims = path.data().dims();
      return new Synth(Normalizer.rename(Term.mkLam(dims.view(),
        new Term.PCall(synth.wellTyped, dims.map(Term.Ref::new), path.data()))),
        Term.mkPi(dims.map(x -> new Param<>(x, Term.I)), path.data().type()));
    } else return new Synth(synth.wellTyped, type);
  }

  private @NotNull Normalizer jonSterling(SeqView<LocalVar> dims, Boundary.Face face) {
    return new Normalizer(sigma, MutableMap.from(dims
      .zip(face.pats()).filter(p -> p._2 != Boundary.Case.VAR)
      .map(p -> Tuple.of(p._1, Term.end(p._2 == Boundary.Case.LEFT)))));
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
