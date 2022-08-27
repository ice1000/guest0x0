import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.util.SPE;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeclsTest {
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

  private static @NotNull Elaborator tyck(@Language("TEXT") String s) {
    return CliMain.tyck(s, false);
  }
}
