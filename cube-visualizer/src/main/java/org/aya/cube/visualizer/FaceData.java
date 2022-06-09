package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeBool;
import org.ice1000.jimgui.NativeInt;
import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

public record FaceData(
  @NotNull NativeInt status,
  @NotNull NativeString latex
) implements AutoCloseable {
  public FaceData() {
    this(new NativeInt(), new NativeString());
  }

  public enum Status {
    Invisible, Shaded, Lines
  }

  public boolean enabled() {
    return status.accessValue() != Status.Invisible.ordinal();
  }

  @Override public void close() {
    status.close();
    latex.close();
  }
}
