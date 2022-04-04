package org.aya.guest0x0.syntax;

import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr {
  @NotNull SourcePos pos();
  record Ref(@Override @NotNull SourcePos pos, @NotNull String name) implements Expr {
  }

  record App(
    @Override @NotNull SourcePos pos,
    @NotNull Expr f,
    @NotNull Expr a
  ) implements Expr {
  }

  record Lam(
    @Override @NotNull SourcePos pos,
    @NotNull String x,
    @NotNull Expr a
  ) implements Expr {
  }

  record U(@Override @NotNull SourcePos pos) implements Expr {
  }
}
