package org.aya.guest0x0.cli;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.aya.guest0x0.parser.Guest0x0Lexer;
import org.aya.guest0x0.parser.Guest0x0Parser;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CliMain {
  public static void main(String @NotNull ... args) throws IOException {
    var ak = tyck(Files.readString(Paths.get(args[0])), true);
    System.out.println("Tycked " + ak.sigma().size() + " definitions, phew.");
  }

  public static @NotNull Elaborator andrasKovacs() {
    return new Elaborator(MutableMap.create(), MutableMap.create());
  }

  public static @NotNull Guest0x0Parser parser(String s) {
    return new Guest0x0Parser(new CommonTokenStream(new Guest0x0Lexer(CharStreams.fromString(s))));
  }

  public static @NotNull ImmutableSeq<Def<Expr>> def(String s) {
    var decls = ImmutableSeq.from(parser(s).program().decl());
    var edj = new Resolver(MutableMap.create());
    return decls.map(d -> edj.def(new Parser(new SourceFile("<input>", Option.none(), s)).def(d)));
  }

  public static @NotNull Elaborator tyck(String code, boolean verbose) {
    var artifact = def(code);
    var akJr = andrasKovacs();
    for (var def : artifact) {
      var tycked = akJr.def(def);
      akJr.sigma().put(tycked.name(), tycked);
      if (verbose) System.out.println(tycked.name());
    }
    return akJr;
  }
}
