package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Param;
import org.aya.guest0x0.util.SPE;
import org.aya.pretty.doc.Doc;
import org.jetbrains.annotations.NotNull;

public record Resolver(@NotNull MutableMap<String, LocalVar> env) {
  private @NotNull TeleCache mkCache(int initialCapacity) {
    return new TeleCache(this, MutableArrayList.create(initialCapacity), MutableArrayList.create(initialCapacity));
  }

  private record TeleCache(Resolver ctx, MutableArrayList<LocalVar> recover, MutableArrayList<LocalVar> remove) {
    private void add(@NotNull LocalVar var) {
      var put = ctx.put(var);
      if (put.isDefined()) recover.append(put.get());
      else remove.append(var);
    }

    private void purge() {
      remove.forEach(key -> ctx.env.remove(key.name()));
      recover.forEach(ctx::put);
    }
  }

  public @NotNull Param<Expr> param(@NotNull Param<Expr> param) {
    return new Param<>(param.x(), expr(param.type()));
  }

  public @NotNull Def<Expr> def(@NotNull Def<Expr> def) {
    return switch (def) {
      case Def.Fn<Expr> fn -> {
        var telescope = MutableArrayList.<Param<Expr>>create(fn.telescope().size());
        var cache = mkCache(fn.telescope().size());
        for (var param : fn.telescope()) {
          telescope.append(new Param<>(param.x(), expr(param.type())));
          cache.add(param.x());
        }
        var result = expr(fn.result());
        put(fn.name());
        var body = expr(fn.body());
        cache.purge();
        yield new Def.Fn<>(fn.name(), telescope.toImmutableArray(), result, body);
      }
    };
  }

  public @NotNull Expr expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.DT dt -> new Expr.DT(dt.isPi(), dt.pos(), param(dt.param()), bodied(param(dt.param()).x(), dt.cod()));
      case Expr.Two two -> new Expr.Two(two.isApp(), two.pos(), expr(two.f()), expr(two.a()));
      case Expr.Lam lam -> new Expr.Lam(lam.pos(), lam.x(), bodied(lam.x(), lam.a()));
      case Expr.UI ui -> ui;
      case Expr.Hole hole -> new Expr.Hole(hole.pos(), env.valuesView().toImmutableSeq());
      case Expr.Unresolved unresolved -> env.getOption(unresolved.name())
        .map(x -> new Expr.Resolved(unresolved.pos(), x))
        .getOrThrow(() -> new SPE(unresolved.pos(), Doc.english("Unresolved: " + unresolved.name())));
      case Expr.Resolved resolved -> resolved;
      case Expr.Proj proj -> new Expr.Proj(proj.pos(), expr(proj.t()), proj.isOne());
      case Expr.Path path -> {
        var dims = path.data().dims();
        var state = mkCache(dims.size());
        dims.forEach(state::add);
        var data = path.data().fmap(this::expr);
        state.purge();
        yield new Expr.Path(path.pos(), data);
      }
      case Expr.Formula f -> new Expr.Formula(f.pos(), f.formula().fmap(this::expr));
      case Expr.Transp transp -> {
        var transpVars = transp.vars().map(v -> env.getOrThrow(v.name(), () ->
          new SPE(transp.pos(), Doc.english("Unresolved: " + v.name()))));
        yield new Expr.Transp(transp.pos(), expr(transp.cover()), transpVars, transp.faces());
      }
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
