package org.aya.cube.visualizer;

import org.aya.cube.compiler.CompiledLine;
import org.ice1000.jimgui.NativeBool;
import org.jetbrains.annotations.NotNull;

public record LineData(
  @NotNull NativeBool isHidden,
  @NotNull NativeBool isDashed,
  @NotNull NativeBool isEqual
) implements AutoCloseable {
  public LineData() {
    this(new NativeBool(), new NativeBool(), new NativeBool());
  }

  @Override public void close() {
    isHidden.close();
    isDashed.close();
    isEqual.close();
  }

  public @NotNull CompiledLine serialize() {
    return new CompiledLine(
      isHidden.accessValue(),
      isDashed.accessValue(),
      isEqual.accessValue());
  }

  public void deserialize(@NotNull CompiledLine serialized) {
    isHidden.modifyValue(serialized.isHidden());
    isDashed.modifyValue(serialized.isDashed());
    isEqual.modifyValue(serialized.isEqual());
  }
}
