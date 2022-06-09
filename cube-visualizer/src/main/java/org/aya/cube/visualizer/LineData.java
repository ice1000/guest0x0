package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeBool;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;

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

  public @NotNull Serialized serialize() {
    return new Serialized(
      isHidden.accessValue(),
      isDashed.accessValue(),
      isEqual.accessValue());
  }

  public void deserialize(@NotNull Serialized serialized) {
    isHidden.modifyValue(serialized.isHidden);
    isDashed.modifyValue(serialized.isDashed);
    isEqual.modifyValue(serialized.isEqual);
  }

  public record Serialized(boolean isHidden, boolean isDashed, boolean isEqual) implements Serializable {
    public void buildText(@NotNull TextBuilder builder, CubeData.Side side, boolean isHighlight) {
      if (isHidden) return;
      var attrs = new ArrayList<String>();
      if (isEqual) attrs.add("equals arrow");
      if (isDashed) attrs.add("dashed");
      builder.appendln("\\draw " + attrs +
        " (" + side.from +
        ") -- (" + side.to + ") ;", isHighlight);
    }
  }
}
