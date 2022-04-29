package org.aya.guest0x0.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public sealed interface Restr<E> {
  /** I'm sorry, I'm just too bad at writing while loops */
  static <T> void combineRecursively(
    SeqView<Formula.Conn<T>> localOrz,
    MutableStack<Cond<T>> conds,
    MutableList<Cofib<T>> combined
  ) {
    if (localOrz.isEmpty()) {
      combined.append(new Cofib<>(conds.toImmutableArray()));
      return;
    }
    var conn = localOrz.first();
    var lateDropped = localOrz.drop(1);
    if (conn.isAnd()) { // a /\ b = 0 ==> a = 0 \/ b = 0
      conds.push(new Cond<>(conn.l(), true));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Cond<>(conn.r(), true));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
    } else { // a \/ b = 1 ==> a = 1 \/ b = 1
      conds.push(new Cond<>(conn.l(), false));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Cond<>(conn.r(), false));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
    }
  }
  Restr<E> fmap(@NotNull Function<E, E> g);
  Restr<E> or(Cond<E> cond);
  <T> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f);
  record Vary<E>(@NotNull ImmutableSeq<Cofib<E>> orz) implements Restr<E> {
    @Override public Vary<E> fmap(@NotNull Function<E, E> g) {
      return new Vary<>(orz.map(x -> x.rename(g)));
    }

    @Override public Vary<E> or(Cond<E> cond) {
      return new Vary<>(orz.appended(new Cofib<>(ImmutableSeq.of(cond))));
    }

    @Override public <T> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Vary<>(orz.map(x -> new Cofib<>(x.ands.map(f))));
    }
  }
  record Const<E>(boolean isTrue) implements Restr<E> {
    @Override public Const<E> fmap(@NotNull Function<E, E> g) {
      return this;
    }

    @Override public Restr<E> or(Cond<E> cond) {
      return isTrue ? this : new Vary<>(ImmutableSeq.of(new Cofib<>(ImmutableSeq.of(cond))));
    }

    @Override public <T> Const<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Const<>(isTrue);
    }
  }
  record Cond<E>(@NotNull E inst, boolean isLeft) {
    public Cond<E> rename(@NotNull Function<E, E> g) {
      return new Cond<>(g.apply(inst), isLeft);
    }
  }
  static @Nullable Formula<Term> formulaOf(@NotNull Cond<Term> cond) {
    return cond.inst instanceof Term.Mula mula ? mula.formula() : null;
  }
  record Cofib<E>(@NotNull ImmutableSeq<Cond<E>> ands) {
    public Cofib<E> rename(@NotNull Function<E, E> g) {
      return new Cofib<>(ands.map(c -> c.rename(g)));
    }
  }
}
