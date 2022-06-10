package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

public record CompiledFace(int status, byte @NotNull [] latex) implements Serializable {
  public void buildText(@NotNull TextBuilder builder, Orient orient, boolean isHighlight) {
    if (latex.length != 0) {
      builder.append("\\node (" + orient.name() +
        ") at (" + orient.center +
        ") {", isHighlight);
      builder.append(latex, isHighlight);
      builder.appendln("} ;", isHighlight);
    }
    switch (Status.values()[status]) {
      case Shaded -> builder.appendln("\\draw [draw=white,line width=3pt,fill=black!50,fill opacity=0.5]", isHighlight);
      case Lines -> builder.appendln("\\fill [pattern color=gray,pattern=north west lines]", isHighlight);
      case Invisible -> {
        return;
      }
    }
    builder.appendln(orient.tikz + " -- cycle ;", isHighlight);
    builder.appendln("% ^ " + orient.name(), isHighlight);
  }

  public enum Orient {
    Top(new int[]{0b000, 0b100, 0b101, 0b001}),
    Bottom(new int[]{0b010, 0b110, 0b111, 0b011}),
    Front(new int[]{0b001, 0b101, 0b111, 0b011}),
    Back(new int[]{0b000, 0b100, 0b110, 0b010}),
    Left(new int[]{0b000, 0b010, 0b011, 0b001}),
    Right(new int[]{0b100, 0b110, 0b111, 0b101});

    public final int @NotNull [] vertices;
    public final @NotNull String tikz;
    public final @NotNull String center;

    public boolean contains(int i) {
      return Arrays.stream(vertices).anyMatch(v -> v == i);
    }

    Orient(int @NotNull [] vertices) {
      this.vertices = vertices;
      tikz = Arrays.stream(vertices).mapToObj(i -> Util.apply3D(i, ($, x, y, z) -> x + "," + y + "," + z))
        .collect(Collectors.joining(") -- (", "(", ")"));
      var sums = new int[]{0, 0, 0};
      Arrays.stream(vertices).forEach(i -> Util.apply3D(i, ($, x, y, z) -> {
        sums[0] += x;
        sums[1] += y;
        return sums[2] += z;
      }));
      center = sums[0] / 4F + "," + sums[1] / 4F + "," + sums[2] / 4F;
    }
  }
  public enum Status {
    Invisible, Shaded, Lines
  }
}
