package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CliMain {
  public static void main(String @NotNull ... args) throws IOException, ClassNotFoundException {
    var in = "cubes.bin";
    var out = "cubes.tex";
    if (args.length >= 1) in = args[0];
    if (args.length >= 2) out = args[1];
    var builder = new TextBuilder.Strings();
    Util.tryLoad(Paths.get(in)).buildText(builder, null);
    Files.writeString(Paths.get(out), builder.sb(), StandardCharsets.US_ASCII, StandardOpenOption.WRITE);
  }
}
