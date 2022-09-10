package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.util.Param;
import org.jetbrains.annotations.NotNull;

public sealed interface Def extends FnLike {
  @NotNull DefVar<? extends Def> name();

  record Fn(
    @Override @NotNull DefVar<Fn> name,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def {
    public Fn {
      name.core = this;
    }
  }

  /**
   * For (maybe mutually) recursive definitions, like types and functions
   *
   * @param isData it will be a function if false
   */
  record Signature(
    boolean isData,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result
  ) implements FnLike {
  }

  record Print(
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def {
    @Override public @NotNull DefVar<? extends Print> name() {
      throw new UnsupportedOperationException();
    }
  }
}
