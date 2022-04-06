package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.DynamicArray;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Param;
import org.jetbrains.annotations.NotNull;

public record Resolver(@NotNull MutableMap<String, LocalVar> env) {
  public @NotNull Param<Expr> param(@NotNull Param<Expr> param) {
    return new Param<>(param.x(), expr(param.type()));
  }

  public @NotNull Def<Expr> def(@NotNull Def<Expr> def) {
    return switch (def) {
      case Def.Fn<Expr> fn -> {
        var telescope = DynamicArray.<Param<Expr>>create(fn.telescope().size());
        var toRecover = DynamicArray.<LocalVar>create(fn.telescope().size());
        var toRemove = DynamicArray.<LocalVar>create(fn.telescope().size());
        for (var param : def.telescope()) {
          var ty = expr(param.type());
          telescope.append(new Param<>(param.x(), ty));
          var put = put(param.x());
          if (put.isDefined()) toRecover.append(put.get());
          else toRemove.append(param.x());
        }
        var result = expr(fn.result());
        put(fn.name());
        var body = expr(fn.body());
        toRemove.forEach(key -> env.remove(key.name()));
        toRecover.forEach(this::put);
        yield new Def.Fn<>(fn.name(), telescope.toImmutableArray(), result, body);
      }
    };
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
      case Expr.Proj proj -> new Expr.Proj(proj.pos(), expr(proj.t()), proj.isOne());
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
