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
      case Expr.DT dt -> {
        var p = param(dt.param());
        var old = put(p.x());
        var cod = expr(dt.cod());
        recover(old, p.x().name());
        yield new Expr.DT(dt.isPi(), dt.pos(), p, cod);
      }
      case Expr.Two two -> new Expr.Two(two.isApp(), two.pos(), expr(two.f()), expr(two.a()));
      case Expr.Lam lam -> {
        var old = put(lam.x());
        var a = expr(lam.a());
        recover(old, lam.x().name());
        yield new Expr.Lam(lam.pos(), lam.x(), a);
      }
      case Expr.Trebor trebor -> trebor;
      case Expr.Unresolved unresolved -> env.getOption(unresolved.name())
        .map(x -> new Expr.Resolved(unresolved.pos(), x))
        .getOrThrow(() -> new RuntimeException("unresolved: " + unresolved.name()));
      case Expr.Resolved resolved -> resolved;
      case Expr.Proj proj -> new Expr.Proj(proj.pos(), expr(proj.t()), proj.oneOrTwo());
    };
  }

  private void recover(Option<LocalVar> old, String name) {
    old.map(this::put).getOrElse(() -> env.remove(name));
  }

  private @NotNull Option<LocalVar> put(LocalVar x) {
    return env.put(x.name(), x);
  }
}
