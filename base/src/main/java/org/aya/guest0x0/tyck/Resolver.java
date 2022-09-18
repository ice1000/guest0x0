package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.guest0x0.syntax.Decl;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.util.AnyVar;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
import org.aya.guest0x0.util.SPE;
import org.aya.pretty.doc.Doc;
import org.jetbrains.annotations.NotNull;

public record Resolver(@NotNull MutableMap<String, AnyVar> env) {
  private @NotNull TeleCache mkCache(int initialCapacity) {
    return new TeleCache(this, MutableArrayList.create(initialCapacity), MutableArrayList.create(initialCapacity));
  }

  private record TeleCache(Resolver ctx, MutableArrayList<AnyVar> recover, MutableArrayList<AnyVar> remove) {
    private void add(@NotNull AnyVar var) {
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

  public @NotNull Decl def(@NotNull Decl def) {
    var tele = tele(def);
    var newTele = new Decl.Tele(tele._1);
    return switch (def) {
      case Decl.Fn fn -> {
        var result = expr(fn.result());
        put(fn.name());
        var body = expr(fn.body());
        tele._2.purge();
        yield new Decl.Fn(fn.name(), newTele, result, body);
      }
      case Decl.Print print -> {
        var result = expr(print.result());
        var body = expr(print.body());
        tele._2.purge();
        yield new Decl.Print(newTele, result, body);
      }
    };
  }

  @NotNull private Tuple2<ImmutableSeq<Param<Expr>>, TeleCache> tele(Decl def) {
    var size = def.tele().scope().size();
    var telescope = MutableArrayList.<Param<Expr>>create(size);
    var cache = mkCache(size);
    for (var param : def.tele().scope()) {
      telescope.append(new Param<>(param.x(), expr(param.type())));
      cache.add(param.x());
    }
    return Tuple.of(telescope.toImmutableArray(), cache);
  }

  public @NotNull Expr expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.DT dt -> new Expr.DT(dt.isPi(), dt.pos(), param(dt.param()), bodied(param(dt.param()).x(), dt.cod()));
      case Expr.Two two -> new Expr.Two(two.isApp(), two.pos(), expr(two.f()), expr(two.a()));
      case Expr.Lam lam -> new Expr.Lam(lam.pos(), lam.x(), bodied(lam.x(), lam.a()));
      case Expr.PrimTy primTy -> primTy;
      case Expr.Hole hole -> new Expr.Hole(hole.pos(),
        env.valuesView().filterIsInstance(LocalVar.class).toImmutableSeq());
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
      case Expr.Mula f -> new Expr.Mula(f.pos(), f.asFormula().fmap(this::expr));
      case Expr.Transp transp -> new Expr.Transp(transp.pos(), expr(transp.cover()), expr(transp.restr()));
      case Expr.Cof cof -> new Expr.Cof(cof.pos(), cof.data().map(this::expr));
      case Expr.PartEl par -> par(par);
      case Expr.PartTy par -> new Expr.PartTy(par.pos(), expr(par.ty()), expr(par.restr()));
      case Expr.Sub sub -> new Expr.Sub(sub.pos(), expr(sub.ty()), par(sub.par()));
      case Expr.SubEl subEl -> new Expr.SubEl(subEl.pos(), expr(subEl.e()), subEl.isIntro());
      case Expr.Hcomp hcomp -> new Expr.Hcomp(hcomp.pos(), hcomp.data().fmap(this::expr));
    };
  }

  private @NotNull Expr.PartEl par(Expr.PartEl par) {
    return new Expr.PartEl(par.pos(), par.clauses().map(clause -> clause.rename(this::expr)));
  }

  private @NotNull Expr bodied(LocalVar x, Expr expr) {
    var old = put(x);
    var e = expr(expr);
    old.map(this::put).getOrElse(() -> env.remove(x.name()));
    return e;
  }

  private @NotNull Option<AnyVar> put(AnyVar x) {
    return env.put(x.name(), x);
  }
}
