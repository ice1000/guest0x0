package org.aya.cube.visualizer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface Constants {
  @NotNull @NonNls String carloPreamble = """
    \\pgfdeclarelayer{frontmost}
    \\pgfsetlayers{main,frontmost}
    \\usetikzlibrary{patterns}

    \\tikzset{
      carlo-axes/.style = { y = {(0,-1)}, z = {(-0.6,0.6)} } ,
      shorten <>/.style = { shorten >=#1 , shorten <=#1 } ,
      equals arrow/.style = {
        arrows = - ,
        double equal sign distance ,
      } ,
    }

    \\newcommand{\\carloTikZ}[1]{
      \\begin{tikzpicture}[carlo-axes, scale = 2, arrows = ->, baseline={(r.base)}]
      #1
      \\end{tikzpicture}}
    """;
}
