package org.aya.guest0x0.syntax;

import org.aya.guest0x0.util.LocalVar;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

public record Param<Term extends Docile>(@NotNull LocalVar x, @NotNull Term type) implements Docile {
  public @NotNull Doc toDoc() {
    return Doc.parened(Doc.sep(Doc.symbol(x.name()), Doc.symbol(":"), type.toDoc()));
  }
}
