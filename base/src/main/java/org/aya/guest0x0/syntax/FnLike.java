package org.aya.guest0x0.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
import org.jetbrains.annotations.NotNull;

public interface FnLike {
  @NotNull ImmutableSeq<Param<Term>> telescope();
  @NotNull Term result();
  default @NotNull SeqView<LocalVar> teleVars() {
    return telescope().view().map(Param::x);
  }
  default @NotNull SeqView<Term> teleRefs() {
    return teleVars().map(Term.Ref::new);
  }
}
