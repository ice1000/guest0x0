package org.aya.cube.visualizer;

import org.aya.cube.compiler.CompiledFace;
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

  public boolean enabled() {
    return status.accessValue() != CompiledFace.Status.Invisible.ordinal();
  }

  @Override public void close() {
    status.close();
    latex.close();
  }

  public @NotNull CompiledFace serialize() {
    return new CompiledFace(status.accessValue(), latex.toBytes());
  }

  public void deserialize(@NotNull CompiledFace serialized) {
    status.modifyValue(serialized.status());
    latex.clear();
    for (var b : serialized.latex()) latex.append(b);
  }
}
