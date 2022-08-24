package org.aya.guest0x0.cubical;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface Formula<E> {
  <T> @NotNull Formula<T> fmap(@NotNull Function<E, T> f);
  /** @param isAnd it's or if false */
  record Conn<E>(boolean isAnd, @NotNull E l, @NotNull E r) implements Formula<E> {
    public <T> @NotNull Conn<T> fmap(@NotNull Function<E, T> f) {return new Conn<>(isAnd, f.apply(l), f.apply(r));}
  }
  record Inv<E>(@NotNull E i) implements Formula<E> {
    public <T> @NotNull Inv<T> fmap(@NotNull Function<E, T> f) {return new Inv<>(f.apply(i));}
  }
  record Lit<E>(boolean isLeft) implements Formula<E> {
    public <T> @NotNull Lit<T> fmap(@NotNull Function<E, T> f) {return new Lit<>(isLeft);}
  }
}
