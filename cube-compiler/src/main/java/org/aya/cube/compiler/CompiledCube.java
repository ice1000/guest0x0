package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public record CompiledCube(
  byte @NotNull [] name,
  @NotNull CompiledPoint @NotNull [] vertices,
  @NotNull CompiledLine @NotNull [] lines,
  @NotNull CompiledFace @NotNull [] faces
) implements Serializable {
  public void buildText(@NotNull TextBuilder builder, Object highlight) {
    builder.appendln("\\carloTikZ{", false);
    builder.append("%<*", false);
    builder.append(name, false);
    builder.appendln(">", false);

    builder.appendln("\\begin{pgfonlayer}{frontmost}\\begin{scope}[every node/.style = {inner sep = 0pt}]", false);
    Util.forEach3D((i, x, y, z) -> {
      var isHighlight = highlight == Integer.valueOf(i);
      builder.append("\\node (" + Util.binPad3(i) +
          ") at (" + x + "," + y + "," + z + ") {\\(",
        isHighlight);
      builder.append(vertices[i].latex(), isHighlight);
      builder.appendln("\\)};", isHighlight);
      return null;
    });
    builder.appendln("\\end{scope}\\end{pgfonlayer}", false);
    builder.appendln("\\begin{scope}[transparency group=knockout]", false);
    for (var orient : CompiledFace.Orient.values()) {
      if (orient.contains(0b010))
        faces[orient.ordinal()].buildText(builder, orient, highlight == orient);
    }
    for (var side : CompiledLine.Side.values()) {
      if (side.isLowerLayer())
        lines[side.ordinal()].buildText(builder, side, highlight == side);
    }
    builder.appendln("\\end{scope}", false);
    builder.appendln("\\begin{scope}[transparency group=knockout]", false);
    for (var orient : CompiledFace.Orient.values()) {
      if (!orient.contains(0b010))
        faces[orient.ordinal()].buildText(builder, orient, highlight == orient);
    }
    for (var side : CompiledLine.Side.values()) {
      if (!side.isLowerLayer())
        lines[side.ordinal()].buildText(builder, side, highlight == side);
    }
    builder.appendln("\\end{scope}", false);
    builder.append("%</", false);
    builder.append(name, false);
    builder.appendln(">", false);
    builder.appendln("}", false);
  }

}
