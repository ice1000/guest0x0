package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public sealed interface Restr<E> {
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
