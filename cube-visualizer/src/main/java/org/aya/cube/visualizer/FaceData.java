package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeBool;
import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

public record FaceData(
  @NotNull NativeBool isEnabled,
  @NotNull NativeString latex
) implements AutoCloseable {
  public FaceData() {
    this(new NativeBool(), new NativeString());
  }

  public boolean enabled() {
    return isEnabled.accessValue();
  }

  @Override public void close() {
    isEnabled.close();
    latex.close();
  }
}
