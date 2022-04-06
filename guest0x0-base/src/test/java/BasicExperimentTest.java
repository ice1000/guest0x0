import kala.collection.mutable.MutableMap;
import kala.control.Either;
import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.cli.Parser;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.util.error.SourceFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicExperimentTest {
  @Test public void resolveId() {
    var e = (Expr.Lam) expr("\\x. x");
    assertNotNull(e);
    assertSame(((Expr.Resolved) e.a()).ref(), e.x());
  }

  @Test public void tyckId() {
    var artifact = tyckExpr("\\A. \\x. x", "Pi (A : Type) -> A -> A");
    assertNotNull(artifact);
  }

  @Test public void tyckUncurry() {
    var artifact = tyckExpr("\\A.\\B.\\C.\\t.\\f. f (t.1) (t.2)",
      """
        Pi (A : Type) -> Pi (B : Type) -> Pi (C : Type) ->
          Pi (t : A ** B) -> Pi (f : A -> B -> C) -> C""");
    assertNotNull(artifact);
  }

  @Test public void fnDef() {
    var artifact = CliMain.def("def uncurry (A : Type) (B : Type) (C : Type)" +
      "(t : A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)").first();
    var tycked = CliMain.andrasKovacs().def(artifact);
    assertNotNull(tycked);
  }

  @Test public void dontSayLazy() {
    var akJr = CliMain.tyck("""
      def uncurry (A : Type) (B : Type) (C : Type)
        (t : A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)
      def uncurry' (A : Type) (t : A ** A) (f : A -> A -> A) : A => uncurry A A A t f
      """);
    akJr.sigma().valuesView().forEach(tycked -> {
      var body = ((Def.Fn<Term>) tycked).body();
      assertTrue(akJr.normalize(body) instanceof Term.Two two && two.isApp());
    });
  }

  @Test public void leibniz() {
    var akJr = CliMain.tyck("""
      def Eq (A : Type) (a : A) (b : A) : Type => Pi (P : A -> Type) -> P a -> P b
      def refl (A : Type) (a : A) : Eq A a a => \\P. \\pa. pa
      def sym (A : Type) (a : A) (b : A) (e : Eq A a b) : Eq A b a => e (\\b. Eq A b a) (refl A a)
      """);
    assertEquals(3, akJr.sigma().size());
  }

  private @NotNull Term tyckExpr(String term, String type) {
    var akJr = CliMain.andrasKovacs();
    var Id = akJr.synth(expr(type));
    return akJr.inherit(expr(term), Id.wellTyped());
  }

  private @NotNull Expr expr(String s) {
    return new Resolver(MutableMap.create())
      .expr(new Parser(Either.left(SourceFile.NONE)).expr(CliMain.parser(s).expr()));
  }
}
