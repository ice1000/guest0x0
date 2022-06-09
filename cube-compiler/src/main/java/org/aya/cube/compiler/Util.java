package org.aya.cube.compiler;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
      \\begin{tikzpicture}[carlo-axes, scale = 2, arrows = ->]
      #1
      \\end{tikzpicture}}
    """;
  static @NotNull String binPad3(int i) {
    // https://stackoverflow.com/a/4421438/7083401
    return String.format("%3s", Integer.toString(i, 2)).replace(' ', '0');
  }

  @FunctionalInterface
  interface Action3D<T> {
    T apply(int i, int x, int y, int z);
  }

  //     _______ x
  //    /|
  //   / |
  //  /  |
  // z   y
  static void forEach3D(@NotNull Action3D<?> action) {
    for (var i = 0; i <= 0b111; ++i) apply3D(i, action);
  }

  static <T> T apply3D(int i, @NotNull Action3D<T> action) {
    var zz = (i & 0b001) > 0;
    var yy = (i & 0b010) > 0;
    var xx = (i & 0b100) > 0;
    var z = zz ? 1 : 0;
    var y = yy ? 1 : 0;
    var x = xx ? 1 : 0;
    return action.apply(i, x, y, z);
  }

  static @NotNull CompiledCube tryLoad(@NotNull Path path) throws IOException, ClassNotFoundException {
    return (CompiledCube) new ObjectInputStream(Files.newInputStream(path)).readObject();
  }

  static @NotNull CompiledCube load(@NotNull Path path) {
    try {
      return tryLoad(path);
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  static void trySave(@NotNull Path path, @NotNull CompiledCube cube) throws IOException {
    new ObjectOutputStream(Files.newOutputStream(path)).writeObject(cube);
  }

  static void save(@NotNull Path path, @NotNull CompiledCube cube) {
    try {
      trySave(path, cube);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
