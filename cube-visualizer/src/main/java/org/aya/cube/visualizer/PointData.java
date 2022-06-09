package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public record PointData(
  @NotNull NativeString latex
) implements AutoCloseable {
  public static final byte[] BULLET = "\\bullet".getBytes(StandardCharsets.US_ASCII);

  public PointData() {
    this(new NativeString());
    for (var b : BULLET) latex.append(b);
    latex.append('\0');
  }

  @Override public void close() {
    latex.close();
  }
}
