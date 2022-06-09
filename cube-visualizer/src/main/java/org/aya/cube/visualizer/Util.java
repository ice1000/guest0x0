package org.aya.cube.visualizer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public interface Util {
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
  static @NotNull String binPad3(int i) {
    // https://stackoverflow.com/a/4421438/7083401
    return String.format("%3s", Integer.toString(i, 2)).replace(' ', '0');
  }

  @FunctionalInterface
  interface Action3D {
    void apply(int i, int x, int y, int z);
  }

  //     _______ x
  //    /|
  //   / |
  //  /  |
  // z   y
  static void forEach3D(@NotNull Action3D action) {
    for (var i = 0; i <= 0b111; ++i) {
      var zz = (i & 0b001) > 0;
      var yy = (i & 0b010) > 0;
      var xx = (i & 0b100) > 0;
      var z = zz ? 1 : 0;
      var y = yy ? 1 : 0;
      var x = xx ? 1 : 0;
      action.apply(i, x, y, z);
    }
  }
}
