package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.DynamicArray;
import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record Elaborator(
  MutableMap<LocalVar, Def<Term>> sigma,
  MutableMap<LocalVar, Term> gamma
) {
  public record Synth(Term wellTyped, Term type) {
  }

  public Term inherit(Expr expr, Term type) {
    return switch (expr) {
      case Expr.Lam lam -> {
        if (!(Normalizer.f(type) instanceof Term.DT dt) || !dt.isPi())
          throw new IllegalArgumentException("Expects a right adjoint to type " + expr + ", got: " + type);
        var body = hof(lam.x(), dt.param().type(), () -> inherit(lam.a(), dt.codomain(new Term.Ref(lam.x()))));
        yield new Term.Lam(new Param<>(lam.x(), dt.param().type()), body);
      }
      case Expr.Two two && !two.isApp() -> {
        if (!(Normalizer.f(type) instanceof Term.DT dt) || dt.isPi())
          throw new IllegalArgumentException("Expects a left adjoint to type " + expr + ", got: " + type);
        var lhs = inherit(two.f(), dt.param().type());
        yield new Term.Two(false, lhs, inherit(two.a(), dt.codomain(lhs)));
      }
      default -> {
        var synth = synth(expr);
        if (!Unifier.untyped(synth.type, type))
          throw new IllegalArgumentException("Expects type " + type + ", got: " + synth.type);
        yield synth.wellTyped;
      }
    };
  }

  public Synth synth(Expr expr) {
    return switch (expr) {
      case Expr.Trebor u -> new Synth(Term.U, Term.U);
      case Expr.Resolved resolved -> new Synth(new Term.Ref(resolved.ref()), gamma.get(resolved.ref()));
      case Expr.Proj proj -> {
        var t = synth(proj.t());
        if (!(t.type instanceof Term.DT dt) || dt.isPi())
          throw new IllegalArgumentException("Expects a left adjoint, got: " + t.type);
        var fst = new Term.Proj(t.wellTyped, true);
        if (proj.isOne()) yield new Synth(fst, dt.param().type());
        yield new Synth(new Term.Proj(t.wellTyped, false), dt.codomain(fst));
      }
      case Expr.Two two -> {
        var f = synth(two.f());
        if (two.isApp()) {
          if (!(f.type instanceof Term.DT dt) || !dt.isPi())
            throw new IllegalArgumentException("Expects a right adjoint, got: " + f.type);
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
      default -> throw new IllegalArgumentException("Synthesis failed: " + expr);
    };
  }

  private <T> T hof(@NotNull LocalVar x, @NotNull Term type, @NotNull Supplier<T> t) {
    gamma.put(x, type);
    var ok = t.get();
    gamma.remove(x);
    return ok;
  }

  public void file(ImmutableSeq<Def<Expr>> defs) {
    for (var def : defs) sigma.put(def.name(), def(def));
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
        yield new Def.Fn<>(def.name(), telescope.toImmutableArray(), result, inherit(fn.body(), result));
      }
    };
  }
}
