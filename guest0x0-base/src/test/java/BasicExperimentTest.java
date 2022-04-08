import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.cli.Parser;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Elaborator;
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
    var artifact = tyckExpr("\\A x. x", "Pi (A : U) -> A -> A");
    assertNotNull(artifact);
  }

  @Test public void tyckUncurry() {
    var artifact = tyckExpr("\\A B C t f. f (t.1) (t.2)",
      "Pi (A B C : U) -> Pi (t : A ** B) -> Pi (f : A -> B -> C) -> C");
    assertNotNull(artifact);
  }

  @Test public void fnDef() {
    var artifact = CliMain.def("def uncurry (A B C : U)" +
      "(t : A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)").first();
    var tycked = CliMain.andrasKovacs().def(artifact);
    assertNotNull(tycked);
  }

  @Test public void dontSayLazy() {
    var akJr = tyck("""
      def uncurry (A B C : U)
        (t : A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)
      def uncurry' (A : U) (t : A ** A) (f : A -> A -> A) : A => uncurry A A A t f
      """);
    akJr.sigma().valuesView().forEach(tycked -> {
      var body = ((Def.Fn<Term>) tycked).body();
      assertTrue(akJr.normalize(body) instanceof Term.Two two && two.isApp());
    });
  }

  @Test public void leibniz() {
    var akJr = tyck("""
      def Eq (A : U) (a b : A) : U => Pi (P : A -> U) -> P a -> P b
      def refl (A : U) (a : A) : Eq A a a => \\P pa. pa
      def sym (A : U) (a b : A) (e : Eq A a b) : Eq A b a => e (\\b. Eq A b a) (refl A a)
      """);
    assertEquals(3, akJr.sigma().size());
  }

  @Test public void funExt() {
    var jon = tyck("""
      def Eq (A : U) (a b : A) : U =>
        [| j |] A { | 0 => a | 1 => b }
      def refl (A : U) (a : A) : Eq A a a => \\i. a
      def funExt (A B : U) (f g : A -> B)
                 (p : Pi (a : A) -> Eq B (f a) (g a))
          : Eq (A -> B) f g => \\i a. p a i
      """);
    assertEquals(3, jon.sigma().size());
  }

  private @NotNull Term tyckExpr(String term, String type) {
    var akJr = CliMain.andrasKovacs();
    var Id = akJr.synth(expr(type));
    return akJr.inherit(expr(term), Id.wellTyped());
  }

  private @NotNull Elaborator tyck(String s) {
    return CliMain.tyck(s, false);
  }

  private @NotNull Expr expr(String s) {
    return new Resolver(MutableMap.create())
      .expr(new Parser(SourceFile.NONE).expr(CliMain.parser(s).expr()));
  }
}
