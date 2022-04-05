package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.LocalVar;
import org.jetbrains.annotations.NotNull;

public record Resolver(@NotNull MutableMap<String, LocalVar> env) {
  public @NotNull Expr.Param param(@NotNull Expr.Param param) {
    return new Expr.Param(param.pos(), param.x(), expr(param.type()));
  }

  public @NotNull Expr expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.DT dt -> new Expr.DT(dt.isPi(), dt.pos(), param(dt.param()), bodied(param(dt.param()).x(), dt.cod()));
      case Expr.Two two -> new Expr.Two(two.isApp(), two.pos(), expr(two.f()), expr(two.a()));
      case Expr.Lam lam -> new Expr.Lam(lam.pos(), lam.x(), bodied(lam.x(), lam.a()));
      case Expr.Trebor trebor -> trebor;
      case Expr.Unresolved unresolved -> env.getOption(unresolved.name())
        .map(x -> new Expr.Resolved(unresolved.pos(), x))
        .getOrThrow(() -> new RuntimeException("unresolved: " + unresolved.name()));
      case Expr.Resolved resolved -> resolved;
      case Expr.Proj proj -> new Expr.Proj(proj.pos(), expr(proj.t()), proj.oneOrTwo());
    };
  }

  private @NotNull Expr bodied(LocalVar x, Expr expr) {
    var old = put(x);
    var e = expr(expr);
    old.map(this::put).getOrElse(() -> env.remove(x.name()));
    return e;
  }

  private @NotNull Option<LocalVar> put(LocalVar x) {
    return env.put(x.name(), x);
  }
}
