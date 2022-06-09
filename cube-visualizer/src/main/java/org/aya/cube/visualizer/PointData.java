package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public record PointData(
  @NotNull NativeString latex
) implements AutoCloseable {
  @Override public void close() {
    latex.close();
  }

  public record Axia(boolean x, boolean y, boolean z) implements Serializable {
  }
}
