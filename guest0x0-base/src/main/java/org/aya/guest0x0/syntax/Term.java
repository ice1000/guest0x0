package org.aya.guest0x0.syntax;

import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.tyck.Normalizer;
import org.jetbrains.annotations.NotNull;

public sealed interface Term {
  default @NotNull Term subst(@NotNull LocalVar x, @NotNull Term t) {
    return new Normalizer(MutableMap.of(x, t)).term(this);
  }

  record Param(@NotNull LocalVar x, @NotNull Term type) {
  }

  record Ref(@NotNull LocalVar var) implements Term {
  }

  record Two(boolean isApp, @NotNull Term f, @NotNull Term a) implements Term {
  }

  record Proj(@NotNull Term t, boolean isOne) implements Term {
  }

  record Lam(@NotNull Param param, @NotNull Term body) implements Term {
  }

  record DT(boolean isPi, @NotNull Param param, @NotNull Term cod) implements Term {
  }

  record U() implements Term {
  }
}
