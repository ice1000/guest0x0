package org.aya.guest0x0.cubical;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public sealed interface Formula<E> {
  @NotNull Formula<E> fmap(@NotNull Function<E, E> f);
  /** @param isAnd it's or if false */
  record Conn<E>(boolean isAnd, @NotNull E l, @NotNull E r) implements Formula<E> {
    public @NotNull Conn<E> fmap(@NotNull Function<E, E> f) {return new Conn<>(isAnd, f.apply(l), f.apply(r));}
  }
  record Inv<E>(@NotNull E i) implements Formula<E> {
    public @NotNull Inv<E> fmap(@NotNull Function<E, E> f) {return new Inv<>(f.apply(i));}
  }
  record Lit<E>(boolean isLeft) implements Formula<E> {
    public @NotNull Lit<E> fmap(@NotNull Function<E, E> f) {return this;}
  }
}
