package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImStr;
import org.jetbrains.annotations.NotNull;

public record CubeData(
  @NotNull FaceData @NotNull [] faces
) implements AutoCloseable {
  public CubeData() {
    this(new FaceData[Orient.values().length]);
  }

  public CubeData {
    for (var face : Orient.values()) {
      //noinspection resource
      faces[face.ordinal()] = new FaceData();
    }
  }

  public boolean enabled(Orient orientation) {
    return faces[orientation.ordinal()].enabled();
  }

  @Override public void close() {
    for (var face : faces) face.close();
  }

  public enum Orient {
    Top, Bottom, Front, Back, Left, Right;

    public final @NotNull JImStr toggle;
    public final @NotNull JImStr input;

    Orient() {
      input = new JImStr("##Input" + name());
      toggle = new JImStr("##Toggle" + name());
    }
  }
}
