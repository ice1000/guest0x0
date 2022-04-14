package org.aya.guest0x0.cli;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import org.antlr.v4.runtime.*;
import org.aya.guest0x0.parser.Guest0x0Lexer;
import org.aya.guest0x0.parser.Guest0x0Parser;
import org.aya.guest0x0.prelude.GeneratedVersion;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "guest0x0",
  mixinStandardHelpOptions = true,
  version = "Guest0x0 v" + GeneratedVersion.VERSION_STRING,
  showDefaultValues = true)
public class CliMain implements Callable<Integer> {
  @CommandLine.Parameters(paramLabel = "<input-file>", description = "File to compile")
  public String inputFile;
  @CommandLine.Option(names = {"--st"}, description = "Show the stack trace")
  public boolean stackTrace = false;

  public static void main(String @NotNull ... args) {
    System.exit(new CommandLine(new CliMain()).execute(args));
  }

  @Override public Integer call() throws Exception {
    if (inputFile == null) {
      System.err.println("Guest0x0 " + GeneratedVersion.COMMIT_HASH);
      System.err.println("Use -h for help");
      return 1;
    }
    try {
      var ak = tyck(Files.readString(Paths.get(inputFile)), true);
      System.out.println("Tycked " + ak.sigma().size() + " definitions, phew.");
      return 0;
    } catch (RuntimeException re) {
      System.err.println(re.getMessage());
      if (stackTrace) re.printStackTrace();
      return 2;
    }
  }

  public static @NotNull Elaborator andrasKovacs() {
    return new Elaborator(MutableMap.create(), MutableMap.create());
  }

  public static @NotNull Guest0x0Parser parser(String s) {
    var p = new Guest0x0Parser(new CommonTokenStream(new Guest0x0Lexer(CharStreams.fromString(s))));
    p.removeErrorListeners();
    p.addErrorListener(new Listener());
    return p;
  }

  private static class Listener extends BaseErrorListener {
    @Override public void syntaxError(
      Recognizer<?, ?> r, Object os, int line, int col, String msg,
      RecognitionException e
    ) {
      throw new RuntimeException("line " + line + ":" + col + " " + msg);
    }
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
