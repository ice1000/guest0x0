package org.aya.cube.visualizer;

import org.ice1000.jimgui.NativeInt;
import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

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

  public @NotNull Serialized serialize() {
    return new Serialized(status.accessValue(), latex.toBytes());
  }

  public void deserialize(@NotNull Serialized serialized) {
    status.modifyValue(serialized.status);
    latex.clear();
    for (var b : serialized.latex) latex.append(b);
  }

  public record Serialized(int status, byte @NotNull [] latex) implements Serializable {
    public void buildText(@NotNull TextBuilder builder, CubeData.Orient orient, boolean isHighlight) {
      if (latex.length != 0) {
        builder.append("\\node (" + orient.name() +
          ") at (" + orient.center +
          ") {", isHighlight);
        builder.append(latex, isHighlight);
        builder.appendln("} ;", isHighlight);
      }
      switch (Status.values()[status]) {
        case Shaded ->
          builder.appendln("\\draw [draw=white,line width=3pt,fill=black!50,fill opacity=0.5]", isHighlight);
        case Lines -> builder.appendln("\\fill [pattern color=gray,pattern=north west lines]", isHighlight);
        case Invisible -> {
          return;
        }
      }
      builder.appendln(orient.tikz + " -- cycle ;", isHighlight);
    }
  }
}
