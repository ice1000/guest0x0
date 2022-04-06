package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

public sealed interface Decl {
  record Fn(
    @Override @NotNull LocalVar name,
    @Override @NotNull ImmutableSeq<Expr.Param> telescope,
    @NotNull Expr result,
    @NotNull Expr body
  ) implements Decl {
  }
}
