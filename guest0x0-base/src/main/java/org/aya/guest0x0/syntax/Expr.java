package org.aya.guest0x0.syntax;

import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr {
  @NotNull SourcePos pos();
  record Ref(@Override @NotNull SourcePos pos, @NotNull String name) implements Expr {
  }

  /** @param isApp it's a tuple if false */
  record Two(
    boolean isApp,
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

  record Proj(
    @Override @NotNull SourcePos pos,
    @NotNull Expr t,
    int oneOrTwo
  ) implements Expr {
  }

  record Trebor(@Override @NotNull SourcePos pos) implements Expr {
  }

  /** @param isPi it's a sigma if false */
  record DT(
    boolean isPi,
    @Override @NotNull SourcePos pos,
    @NotNull Param param,
    @NotNull Expr cod
  ) implements Expr {
  }

  record Param(
    @NotNull SourcePos pos,
    @NotNull String x,
    @NotNull Expr type
  ) {
  }
}
