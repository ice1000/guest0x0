package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public sealed interface Def<Term extends Docile> {
  @NotNull ImmutableSeq<Param<Term>> telescope();
  @NotNull LocalVar name();
  @NotNull Term result();

  record Fn<Term extends Docile>(
    @Override @NotNull LocalVar name,
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def<Term> {}

  record Print<Term extends Docile>(
    @Override @NotNull ImmutableSeq<Param<Term>> telescope,
    @NotNull Term result,
    @NotNull Term body
  ) implements Def<Term> {
    public static final @NotNull LocalVar UNUSED = new LocalVar("_");

    @Override public @NotNull LocalVar name() {return UNUSED;}
  }
}
