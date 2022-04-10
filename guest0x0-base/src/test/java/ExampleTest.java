import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.cli.Parser;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.guest0x0.util.SPE;
import org.aya.util.error.SourceFile;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExampleTest {
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
    tyck("""
      def Eq (A : U) (a b : A) : U => Pi (P : A -> U) -> P a -> P b
      def refl (A : U) (a : A) : Eq A a a => \\P pa. pa
      def sym (A : U) (a b : A) (e : Eq A a b) : Eq A b a =>
          e (\\b. Eq A b a) (refl A a)
      """);
  }

  @Test public void funExt() {
    tyck("""
      def Eq (A : U) (a b : A) : U =>
        [| j |] A { | 0 => a | 1 => b }
      def refl (A : U) (a : A) : Eq A a a => \\i. a
      def funExt (A B : U) (f g : A -> B)
                 (p : Pi (a : A) -> Eq B (f a) (g a))
          : Eq (A -> B) f g => \\i a. p a i
      def pmap (A B : U) (f : A -> B) (a b : A) (p : Eq A a b)
          : Eq B (f a) (f b) => \\i. f (p i)
      """);
  }

  @Test public void confluence() {
    assertThrowsExactly(SPE.class, () -> tyck(
      "def feizhu (A : U) (a b : A) : U => [| i j |] A { | 0 _ => a | _ 1 => b }"));
  }

  @Test public void square() {
    tyck("""
      def Eq (A : U) (a b : A) : U =>
        [| j |] A { | 0 => a | 1 => b }
      def EqP (A : I -> U) (a : A 0) (b : A 1) : U =>
        [| j |] A j { | 0 => a | 1 => b }
      def Sq (A : U) (a b c d : A) (ab : Eq A a b) (cd : Eq A c d) : U =>
        [| i j |] A { | 0 _ => ab j | 1 _ => cd j }
      def refl (A : U) (a : A) : Eq A a a => \\i. a
      def SqExm (A : U) (a b c d : A) (ab : Eq A a b) (cd : Eq A c d)
           (sq : Sq A a b c d ab cd) : Eq A a (sq 0 0) => refl A a
      """);
  }

  private @NotNull Term tyckExpr(String term, String type) {
    var akJr = CliMain.andrasKovacs();
    var Id = akJr.synth(expr(type));
    return akJr.inherit(expr(term), Id.wellTyped());
  }

  private @NotNull Elaborator tyck(@Language("TEXT") String s) {
    return CliMain.tyck(s, false);
  }

  private @NotNull Expr expr(String s) {
    return new Resolver(MutableMap.create())
      .expr(new Parser(SourceFile.NONE).expr(CliMain.parser(s).expr()));
  }
}
