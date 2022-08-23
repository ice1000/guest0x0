package org.aya.guest0x0.syntax;

import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record CompData<Expr extends Docile>(
  @NotNull Expr phi,
  @NotNull Expr ty,
  @NotNull Expr walls,
  @NotNull Expr bottom
) implements Docile {
  public <T extends Docile> @NotNull CompData<T> fmap(@NotNull Function<Expr, T> f) {
    return new CompData<>(f.apply(phi), f.apply(ty), f.apply(walls), f.apply(bottom));
  }
  @Override public @NotNull Doc toDoc() {
    return Doc.sep(Doc.plain("hc"), walls.toDoc(), Doc.plain("on"), bottom.toDoc());
  }
}
