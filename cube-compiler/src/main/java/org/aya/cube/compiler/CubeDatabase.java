package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;

public record CubeDatabase(
  byte @NotNull [] customPreamble,
  @NotNull ArrayList<CompiledCube> cubes
) implements Serializable {
  public CubeDatabase() {
    this(new byte[]{}, new ArrayList<>());
  }

  public void buildText(@NotNull TextBuilder builder) {
    builder.appendln(Util.veryPrefix, false);
    builder.append(customPreamble, false);
    builder.appendln(Util.carloPreamble, false);
    builder.appendln("\\begin{document}\\maketitle{}\\tableofcontents{}\\newpage{}", false);
    cubes.forEach(cube -> {
      builder.append("\\section{", false);
      builder.append(cube.name(), false);
      builder.appendln("}\\begin{center}", false);
      cube.buildText(builder, null);
      builder.appendln("\\end{center}", false);
    });
    builder.appendln("\\end{document}", false);
  }
}
