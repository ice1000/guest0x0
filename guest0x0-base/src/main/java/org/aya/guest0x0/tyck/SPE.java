package org.aya.guest0x0.tyck;

import org.aya.pretty.error.PrettyErrorConfig;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

/** Gaolao SPE yoyo, also <code>SourcePosException</code> */
public class SPE extends RuntimeException {
  public final @NotNull SourcePos pos;

  public SPE(@NotNull SourcePos pos, @NotNull String message) {
    super("\n" + message + "\nSource pos: " + pos.toSpan().normalize(PrettyErrorConfig.DEFAULT));
    this.pos = pos;
  }
}
