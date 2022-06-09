package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Paths;

public class CliMain {
  public static void main(String @NotNull ... args) throws IOException, ClassNotFoundException {
    var in = "cubes.bin";
    var out = "cubes.tex";
    if (args.length >= 1) in = args[0];
    if (args.length >= 2) out = args[1];
    Util.main(Paths.get(in), Paths.get(out));
  }
}
