package org.aya.cube.visualizer;

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

  public void buildText(@NotNull TextBuilder builder, CubeData.Orient orient, boolean isHighlight) {
    var st = status.accessValue();
    if (!latex.isEmpty()) {
      builder.append("\\node (" + orient.name() +
        ") at (" + orient.center +
        ") {", isHighlight);
      builder.append(latex, isHighlight);
      builder.appendln("} ;", isHighlight);
    }
    if (st == FaceData.Status.Shaded.ordinal()) {
      builder.appendln("\\draw [draw=white,line width=3pt,fill=black!50,fill opacity=0.5]", isHighlight);
    } else if (st == FaceData.Status.Lines.ordinal()) {
      builder.appendln("\\fill [pattern color=gray,pattern=north west lines]", isHighlight);
    } else return;
    builder.appendln(orient.tikz + " -- cycle ;", isHighlight);
  }
}
