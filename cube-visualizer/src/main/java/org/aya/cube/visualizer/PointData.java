package org.aya.cube.visualizer;

import org.aya.cube.compiler.CompiledPoint;
import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public record PointData(@NotNull NativeString latex) implements AutoCloseable {
  public static final byte[] BULLET = "\\bullet".getBytes(StandardCharsets.US_ASCII);

  public PointData() {
    this(new NativeString());
    for (var b : BULLET) latex.append(b);
  }

  @Override public void close() {
    latex.close();
  }

  public @NotNull CompiledPoint serialize() {
    return new CompiledPoint(latex.toBytes());
  }

  public void deserialize(@NotNull CompiledPoint serialized) {
    latex.clear();
    for (byte b : serialized.latex()) latex.append(b);
  }
}
