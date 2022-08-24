import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.cli.Parser;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExprsTest {
  @Test public void tyckUncurry() {
    var artifact = tyck("\\A B C t f. f (t.1) (t.2)",
      "Pi (A B C : U) -> Pi (t : A ** B) -> Pi (f : A -> B -> C) -> C");
    assertNotNull(artifact);
  }

  @Test public void resolveId() {
    var e = (Expr.Lam) resolve("\\x. x");
    assertNotNull(e);
    assertSame(((Expr.Resolved) e.a()).ref(), e.x());
  }

  @Test public void distill() {
    assertEquals("a b c", distill("a b c"));
    assertEquals("a b c", distill("(a b) c"));
    assertEquals("a (b c)", distill("a (b c)"));
    assertEquals("Pi (_ : a b) -> c d", distill("a b -> c d"));
    assertEquals("Pi (_ : a b) -> Pi (_ : c d) -> e f", distill("a b -> c d -> e f"));
    assertEquals("Pi (_ : a b) -> Pi (_ : c d) -> e f", distill("a b -> (c d -> e f)"));
    assertEquals("Pi (_ : Pi (_ : a b) -> c d) -> e f", distill("(a b -> c d) -> e f"));
    assertEquals("a (~ b c)", distill("a (~ b c)"));
    assertEquals("a (~ c)", distill("a (~ c)"));
    assertEquals("a ((~ c) /\\ b)", distill("a (~ c /\\ b)"));
    assertEquals("a ((~ c d) /\\ b)", distill("a ((~ c d) /\\ b)"));
    assertEquals("a (~ (c d /\\ b))", distill("a (~ (c d /\\ b))"));
  }

  @Test public void parseFail() {
    assertThrows(RuntimeException.class, () -> parse("\\"));
  }

  private static @NotNull String distill(@Language("TEXT") String s) {
    return parse(s).toDoc().debugRender();
  }

  @Test public void tyckId() {
    var artifact = tyck("\\A x. x", "Pi (A : U) -> A -> A");
    assertNotNull(artifact);
  }

  private static @NotNull Term tyck(@Language("TEXT") String term, @Language("TEXT") String type) {
    return tyck(term, type, CliMain.andrasKovacs());
  }

  public static Term tyck(@Language("TEXT") String term, @Language("TEXT") String type, Elaborator akJr) {
    var Id = akJr.synth(resolve(type));
    return akJr.inherit(resolve(term), Id.wellTyped());
  }

  private static @NotNull Expr resolve(String s) {
    return new Resolver(MutableMap.create()).expr(parse(s));
  }

  private static @NotNull Expr parse(String s) {
    return new Parser(SourceFile.NONE).expr(CliMain.parser(s).expr());
  }
}
