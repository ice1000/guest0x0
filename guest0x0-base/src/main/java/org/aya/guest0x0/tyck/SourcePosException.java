package org.aya.guest0x0.tyck;

import org.aya.pretty.error.PrettyErrorConfig;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public class SourcePosException extends RuntimeException {
  public final @NotNull SourcePos pos;

  public SourcePosException(@NotNull SourcePos pos, @NotNull String message) {
    super("\n" + message + "\nsource pos: " + pos.toSpan().normalize(PrettyErrorConfig.DEFAULT));
    this.pos = pos;
  }
}
