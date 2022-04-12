import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.cli.Parser;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ExprsTest {
  @Test public void tyckUncurry() {
    var artifact = tyckExpr("\\A B C t f. f (t.1) (t.2)",
      "Pi (A B C : U) -> Pi (t : A ** B) -> Pi (f : A -> B -> C) -> C");
    assertNotNull(artifact);
  }

  @Test public void resolveId() {
    var e = (Expr.Lam) expr("\\x. x");
    assertNotNull(e);
    assertSame(((Expr.Resolved) e.a()).ref(), e.x());
  }

  @Test public void tyckId() {
    var artifact = tyckExpr("\\A x. x", "Pi (A : U) -> A -> A");
    assertNotNull(artifact);
  }

  static @NotNull Term tyckExpr(String term, String type) {
    var akJr = CliMain.andrasKovacs();
    var Id = akJr.synth(expr(type));
    return akJr.inherit(expr(term), Id.wellTyped());
  }

  static @NotNull Expr expr(String s) {
    return new Resolver(MutableMap.create())
      .expr(new Parser(SourceFile.NONE).expr(CliMain.parser(s).expr()));
  }
}
