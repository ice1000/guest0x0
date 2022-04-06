package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

public sealed interface Def {
  @NotNull ImmutableSeq<Term.Param> telescope();
  @NotNull LocalVar name();

  record Fn(
    @Override @NotNull LocalVar name,
    @Override @NotNull ImmutableSeq<Term.Param> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def {
  }
}
