package org.aya.guest0x0.syntax;

import org.jetbrains.annotations.NotNull;

public sealed interface Term {
  record Param(@NotNull LocalVar x, @NotNull Term type) {
  }

  record Ref(@NotNull LocalVar var) implements Term {
  }

  record App(@NotNull Term f, @NotNull Term arg) implements Term {
  }

  record Lam(@NotNull Param param, @NotNull Term body) implements Term {
  }

  record DT(boolean isPi, @NotNull Param param, @NotNull Term cod) implements Term {
  }

  record U() implements Term {
  }
}
