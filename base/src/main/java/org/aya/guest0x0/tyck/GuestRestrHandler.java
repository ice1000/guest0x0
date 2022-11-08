package org.aya.guest0x0.tyck;

import org.aya.guest0x0.cubical.RestrSimplifier;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GuestRestrHandler implements RestrSimplifier<Term, LocalVar> {
  public static final @NotNull GuestRestrHandler INSTANCE = new GuestRestrHandler();

  @Override public @Nullable LocalVar asRef(Term term) {
    return term instanceof Term.Ref ref ? ref.var() : null;
  }
}
