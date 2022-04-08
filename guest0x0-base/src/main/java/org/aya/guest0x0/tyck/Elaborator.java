package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.DynamicArray;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.syntax.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record Elaborator(
  MutableMap<LocalVar, Def<Term>> sigma,
  MutableMap<LocalVar, Term> gamma
) {
  @NotNull public Term normalize(@NotNull Term term) {
    return new Normalizer(sigma, MutableMap.create()).term(term);
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Term type) {
  }

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
          var lamDims = DynamicArray.<LocalVar>create(tyDims.size());
          var unlam = Expr.unlam(lamDims, tyDims.size(), lam);
          lamDims.forEach(t -> gamma.put(t, Term.I));
          var ty = new Normalizer(sigma, MutableMap.from(
            lamDims.view().zip(tyDims).map(t -> Tuple.of(t._1, new Term.Ref(t._2)))
          )).term(path.data().ty());
          var core = inherit(unlam, ty);
          for (var boundary : path.data().boundaries())
            Unifier.untyped(boundary.body(), jonSterling(tyDims, boundary).term(core));
          yield new Term.PLam(lamDims.toImmutableArray(), core);
        }
        default -> throw new SourcePosException(lam.pos(), "Expects a right adjoint for " + expr + ", got: " + type);
      };
      case Expr.Two two && !two.isApp() -> {
        if (!(normalize(type) instanceof Term.DT dt) || dt.isPi())
          throw new SourcePosException(two.pos(), "Expects a left adjoint to type " + expr + ", got: " + type);
        var lhs = inherit(two.f(), dt.param().type());
        yield new Term.Two(false, lhs, inherit(two.a(), dt.codomain(lhs)));
      }
      default -> {
        var synth = synth(expr);
        if (!Unifier.untyped(normalize(synth.type), normalize(type)))
          throw new SourcePosException(expr.pos(), "Expects type " + type + ", got: " + synth.type);
        yield synth.wellTyped;
      }
    };
  }

  public Synth synth(Expr expr) {
    return switch (expr) {
      case Expr.UI u -> new Synth(new Term.UI(u.isU()), Term.U);
      case Expr.Resolved resolved -> {
        var type = gamma.getOrNull(resolved.ref());
        if (type != null) yield new Synth(new Term.Ref(resolved.ref()), type);
        var def = sigma.get(resolved.ref());
        var pi = Term.mkPi(def.telescope(), def.result());
        yield switch (def) {
          case Def.Fn<Term> fn -> new Synth(Term.mkLam(fn.telescope(), fn.body()), pi);
        };
      }
      case Expr.Proj proj -> {
        var t = synth(proj.t());
        if (!(t.type instanceof Term.DT dt) || dt.isPi())
          throw new SourcePosException(proj.pos(), "Expects a left adjoint, got: " + t.type);
        var fst = new Term.Proj(t.wellTyped, true);
        if (proj.isOne()) yield new Synth(fst, dt.param().type());
        yield new Synth(new Term.Proj(t.wellTyped, false), dt.codomain(fst));
      }
      case Expr.Two two -> {
        var f = synth(two.f());
        if (two.isApp()) {
          if (!(normalize(f.type) instanceof Term.DT dt) || !dt.isPi())
            throw new SourcePosException(two.pos(), "Expects a right adjoint, got: " + f.type);
          var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
          yield new Synth(new Term.Two(true, f.wellTyped, a), dt.codomain(a));
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
        var ty = inherit(path.data().ty(), new Term.UI(true));
        var boundaries = DynamicArray.<Boundary<Term>>create(path.data().boundaries().size());
        for (var boundary : path.data().boundaries()) {
          if (!dims.sizeEquals(boundary.pats())) throw new SourcePosException(
            path.pos(), "Expects " + dims.size() + " patterns, got: " + boundary.pats().size());
          var term = inherit(boundary.body(), jonSterling(dims, boundary).term(ty));
          boundaries.append(new Boundary<>(boundary.pats(), term));
        }
        var data = new Boundary.Data<>(dims, ty, boundaries.toImmutableArray());
        yield new Synth(new Term.Path(data), Term.U);
      }
      default -> throw new SourcePosException(expr.pos(), "Synthesis failed: " + expr);
    };
  }

  @NotNull private Normalizer jonSterling(ImmutableSeq<LocalVar> dims, Boundary<?> boundary) {
    return new Normalizer(sigma, MutableMap.from(dims.view()
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
        var telescope = DynamicArray.<Param<Term>>create(fn.telescope().size());
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
