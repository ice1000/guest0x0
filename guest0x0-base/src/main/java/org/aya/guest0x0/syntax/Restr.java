package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface Restr<E> {
  Restr<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g);
  Restr<E> or(Cond<E> cond);
  <T> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f);
  record Vary<E>(@NotNull ImmutableSeq<Cofib<E>> orz) implements Restr<E> {
    @Override public Vary<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
      return new Vary<>(orz.map(x -> x.rename(f, g)));
    }

    @Override public Vary<E> or(Cond<E> cond) {
      return new Vary<>(orz.appended(new Cofib<>(ImmutableSeq.of(cond))));
    }

    @Override public <T> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Vary<>(orz.map(x -> new Cofib<>(x.ands.map(f))));
    }
  }
  record Const<E>(boolean isTrue) implements Restr<E> {
    @Override public Const<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
      return this;
    }

    @Override public Restr<E> or(Cond<E> cond) {
      var cofib = new Cofib<E>(ImmutableSeq.of(cond));
      return isTrue ? this : new Vary<>(ImmutableSeq.of(cofib));
    }

    @Override public <T> Const<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Const<>(isTrue);
    }
  }
  record Cond<E>(@NotNull LocalVar i, @NotNull E inst, boolean isLeft) {
    public Cond<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
      return new Cond<>(f.apply(i), g.apply(inst), isLeft);
    }
  }
  record Cofib<E>(@NotNull ImmutableSeq<Cond<E>> ands) {
    public Cofib<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
      return new Cofib<>(ands.map(c -> c.rename(f, g)));
    }
  }
}
