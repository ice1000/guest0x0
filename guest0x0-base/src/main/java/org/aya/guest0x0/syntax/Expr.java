package org.aya.guest0x0.syntax;

import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr {
  @NotNull SourcePos pos();
  record Unresolved(@Override @NotNull SourcePos pos, String name) implements Expr {
  }

  record Resolved(@Override @NotNull SourcePos pos, LocalVar ref) implements Expr {
  }

  /** @param isApp it's a tuple if false */
  record Two(boolean isApp, @Override @NotNull SourcePos pos, Expr f, Expr a) implements Expr {
  }

  record Lam(@Override @NotNull SourcePos pos, LocalVar x, Expr a) implements Expr {
  }

  /** @param isOne it's a second projection if false */
  record Proj(@Override @NotNull SourcePos pos, @NotNull Expr t, boolean isOne) implements Expr {
  }

  record Trebor(@Override @NotNull SourcePos pos) implements Expr {
  }

  /** @param isPi it's a sigma if false */
  record DT(boolean isPi, @Override @NotNull SourcePos pos, Param<Expr> param, Expr cod) implements Expr {
  }

  record Path(@Override @NotNull SourcePos pos, @NotNull Boundary.Data<Expr> data) implements Expr {
  }
}
