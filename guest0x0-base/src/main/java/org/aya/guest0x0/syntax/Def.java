package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

public sealed interface Def<Term> {
  @NotNull ImmutableSeq<Param<Term>> telescope();
  @NotNull LocalVar name();

  record Fn<Term>(
    @Override @NotNull LocalVar name,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def<Term> {
  }
}
