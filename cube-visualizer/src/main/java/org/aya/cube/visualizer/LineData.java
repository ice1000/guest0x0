package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeBool;
import org.jetbrains.annotations.NotNull;

public record LineData(
  @NotNull NativeBool isHidden,
  @NotNull NativeBool isDashed,
  @NotNull NativeBool isDoubled
) implements AutoCloseable {
  public LineData() {
    this(new NativeBool(), new NativeBool(), new NativeBool());
  }

  @Override public void close() {
    isHidden.close();
    isDashed.close();
    isDoubled.close();
  }
}
