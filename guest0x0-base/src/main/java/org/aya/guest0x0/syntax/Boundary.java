package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Boundary<E>(@NotNull ImmutableSeq<Case> pats, @NotNull E body) {
  public enum Case {
    LEFT, RIGHT, VAR
  }

  public <T> @NotNull Boundary<T> fmap(@NotNull Function<E, T> f) {
    return new Boundary<>(pats, f.apply(body));
  }

  public record Data<E>(
    @NotNull ImmutableSeq<LocalVar> dims,
    @NotNull E ty,
    @NotNull ImmutableSeq<Boundary<E>> boundaries
  ) {
    public <T> @NotNull Data<T> fmap(@NotNull Function<E, T> f, @NotNull ImmutableSeq<LocalVar> newDims) {
      return new Data<>(newDims, f.apply(ty), boundaries.map(b -> b.fmap(f)));
    }
  }
}

