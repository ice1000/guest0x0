package org.aya.guest0x0.syntax;

import kala.collection.mutable.MutableList;
import org.aya.guest0x0.tyck.SPE;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public sealed interface Expr {
  @NotNull SourcePos pos();
  record Unresolved(@Override @NotNull SourcePos pos, String name) implements Expr {}
  record Resolved(@Override @NotNull SourcePos pos, LocalVar ref) implements Expr {}

  /** @param isApp it's a tuple if false */
  record Two(boolean isApp, @Override @NotNull SourcePos pos, Expr f, Expr a) implements Expr {}
  record Lam(@Override @NotNull SourcePos pos, LocalVar x, Expr a) implements Expr {}

  static @NotNull Expr unlam(@NotNull MutableList<LocalVar> binds, int n, @NotNull Expr body) {
    if (n == 0) return body;
    if (body instanceof Lam lam) {
      binds.append(lam.x);
      return unlam(binds, n - 1, lam.a);
    } else throw new SPE(body.pos(), "Expected (path) lambda");
  }

  /** @param isOne it's a second projection if false */
  record Proj(@Override @NotNull SourcePos pos, @NotNull Expr t, boolean isOne) implements Expr {}

  /** @param isU it's the interval type if false */
  record UI(@Override @NotNull SourcePos pos, boolean isU) implements Expr {}

  /** @param isPi it's a sigma if false */
  record DT(boolean isPi, @Override @NotNull SourcePos pos, Param<Expr> param, Expr cod) implements Expr {}
  record Path(@Override @NotNull SourcePos pos, @NotNull Boundary.Data<Expr> data) implements Expr {}
}
