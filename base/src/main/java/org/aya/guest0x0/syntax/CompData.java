package org.aya.guest0x0.syntax;

import org.aya.guest0x0.util.LocalVar;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public record CompData<Expr extends Docile>(
  @NotNull LocalVar h,
  @NotNull Expr walls,
  @NotNull Expr bottom
) implements Docile {
  @Override public @NotNull Doc toDoc() {
    return Doc.sep(Doc.plain("hc"), Doc.plain(h.name()),
      walls.toDoc(), Doc.plain("on"), bottom.toDoc());
  }
}
