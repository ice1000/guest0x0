package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.util.Param;
import org.jetbrains.annotations.NotNull;

/**
 * Concrete syntax of global definitions.
 */
public sealed interface Decl {
  record Tele(@NotNull ImmutableSeq<Param<Expr>> scope) {
  }
  @NotNull DefVar<? extends Def> name();
  @NotNull Tele tele();
  record Fn(
    @Override @NotNull DefVar<Def.Fn> name,
    @Override @NotNull Tele tele,
    @NotNull Expr result,
    @NotNull Expr body
  ) implements Decl {}
  record Print(
    @Override @NotNull Tele tele,
    @NotNull Expr result,
    @NotNull Expr body
  ) implements Decl {
    @Override public @NotNull DefVar<? extends Def> name() {
      throw new UnsupportedOperationException();
    }
  }
}
