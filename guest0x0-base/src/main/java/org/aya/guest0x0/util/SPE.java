package org.aya.guest0x0.util;

import kala.collection.immutable.ImmutableSeq;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.pretty.error.PrettyErrorConfig;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

/** Gaolao SPE yoyo, also <code>SourcePosException</code> */
public class SPE extends RuntimeException {
  public final @NotNull SourcePos pos;

  public SPE(@NotNull SourcePos pos, @NotNull Doc message) {
    super("\n" + message.renderWithPageWidth(80, false) + "\nSource pos: " +
      pos.toSpan().normalize(PrettyErrorConfig.DEFAULT));
    this.pos = pos;
  }

  public SPE(@NotNull SourcePos pos, Docile @NotNull ... message) {
    this(pos, Doc.sep(ImmutableSeq.from(message).map(Docile::toDoc)));
  }
}
