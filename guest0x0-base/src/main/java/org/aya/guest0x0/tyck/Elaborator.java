package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public record Elaborator(
  @NotNull MutableMap<LocalVar, Term> env
) {
  record Synth(@NotNull Term wellTyped, @NotNull Term type) {
  }

  public @NotNull Term inherit(@NotNull Expr expr, @NotNull Term type) {
    return switch (expr) {
      case Expr.Lam lam -> {
        if (!(Normalizer.f(type) instanceof Term.DT dt) || !dt.isPi())
          throw new IllegalArgumentException("Expects a right adjoint to type " + expr + ", got: " + type);
        env.put(lam.x(), dt.param().type());
        var body = inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())));
        yield new Term.Lam(new Term.Param(lam.x(), dt.param().type()), body);
      }
      case Expr.Two two && !two.isApp() -> {
        if (!(Normalizer.f(type) instanceof Term.DT dt) || dt.isPi())
          throw new IllegalArgumentException("Expects a left adjoint to type " + expr + ", got: " + type);
        var lhs = inherit(two.f(), dt.param().type());
        var rhs = inherit(two.a(), dt.codomain(lhs));
        yield new Term.Two(false, lhs, rhs);
      }
      default -> throw new UnsupportedOperationException("TODO: conversion");
    };
  }

  public @NotNull Synth synth(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.Trebor trebor -> new Synth(new Term.U(), new Term.U());
      case Expr.Resolved resolved -> new Synth(new Term.Ref(resolved.ref()), env.get(resolved.ref()));
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
          param(dt.param());
          var a = inherit(two.a(), dt.param().type());
          yield new Synth(new Term.Two(true, f.wellTyped, a), dt.codomain(a));
        } else {
          var a = synth(two.a());
          yield new Synth(new Term.Two(false, f.wellTyped, a.wellTyped),
            new Term.DT(false, new Term.Param(new LocalVar("_"), f.type), a.type));
        }
      }
      case Expr.DT dt -> {
        var param = param(dt.param());
        var cod = synth(dt.cod());
        yield new Synth(new Term.DT(dt.isPi(), param, cod.wellTyped), cod.type);
      }
      default -> throw new IllegalArgumentException("Synthesis failed: " + expr);
    };
  }

  @NotNull private Term.Param param(Expr.Param ppp) {
    var param = synth(ppp.type());
    env.put(ppp.x(), param.wellTyped);
    return new Term.Param(ppp.x(), param.wellTyped);
  }

  private void param(@NotNull Term.Param param) {
    env.put(param.x(), param.type());
  }
}
