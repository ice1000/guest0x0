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

  @Test public void funExt() {
    tyck("""
      def Eq (A : U) (a b : A) : U =>
        [| j |] A {| j = 0 |-> a | j = 1 |-> b |}
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
      "def feizhu (A : U) (a b : A) : U => [| i j |] A {| i = 0 |-> a | j = 1 |-> b |}"));
  }

  @Test public void boundaries() {
    assertThrowsExactly(SPE.class, () -> tyck(
      "def feizhu (A : U) (a b : A) : [| i |] A {| i = 0 |-> a | i = 1 |-> b |} => \\i. a"));
  }

  @Test public void constantViolation() {
    assertThrowsExactly(SPE.class, () -> tyck(
      "def trauma (A : I -> U) (a : A 0) (i : I) : A i => (\\j. A (i /\\ j)) #{i = 1} a\n"));
  }

  @Test public void connections() {
    tyck("def f (i j : I) : I => (j /\\ i) \\/ j /\\ ~ i");
    tyck("""
      def Eq (A : U) (a b : A) : U =>
        [| j |] A {| j = 0 |-> a | j = 1 |-> b |}
      def sym (A : U) (a b : A) (p : Eq A a b)
        : Eq A b a => \\i. p (~ i)
      def symSym (A : U) (a b : A) (p : Eq A a b)
        : Eq (Eq A a b) p (sym A b a (sym A a b p)) => \\i. p
      def rotate (A : U) (a b : A) (p q : Eq A a b)
                 (s : Eq (Eq A a b) p q)
        : Eq (Eq A b a) (sym A a b q) (sym A a b p)
        => \\i j. s (~ i) (~ j)
      def minSq (A : U) (a b : A) (p : Eq A a b)
        : [| i j |] A {| i = 0 |-> a | i = 1 /\\ j = 0 |-> a | i = 1 /\\ j = 1 |-> b |}
        => \\i j. p (i /\\ j)
      def maxSq (A : U) (a b : A) (p : Eq A a b)
        : [| i j |] A {| i = 0 /\\ j = 0 |-> a | i = 1 /\\ j = 0 |-> b | j = 1 |-> b |}
        => \\i j. p (i \\/ j)
      """);
  }

  @Test public void square() {
    tyck("""
      def Eq (A : U) (a b : A) : U =>
        [| j |] A {| j = 0 |-> a | j = 1 |-> b |}
      def EqP (A : I -> U) (a : A 0) (b : A 1) : U =>
        [| j |] A j {| j = 0 |-> a | j = 1 |-> b |}
      def Sq (A : U) (a b c d : A) (ab : Eq A a b) (cd : Eq A c d) : U =>
        [| i j |] A {| i = 0 |-> ab j | i = 1 |-> cd j |}
      def refl (A : U) (a : A) : Eq A a a => \\i. a
      def SqExm (A : U) (a b c d : A) (ab : Eq A a b) (cd : Eq A c d)
           (sq : Sq A a b c d ab cd) : Eq A a (sq 0 0) => refl A a
      """);
  }

  @Test public void transportTyping() {
    tyck("""
      
      def Eq (A : U) (a b : A) : U =>
        [| j |] A {| j = 0 |-> a | j = 1 |-> b |}
      def trans (A : I -> U) (a : A 0) : A 1 => tr A #{0=1} a
      def transPi (A : I -> U) (B : Pi (i : I) -> A i -> U)
          (f : Pi (x : A 0) -> B 0 x) : Pi (x : A 1) -> B 1 x =>
        \\x. trans (\\j. B j (tr (\\i. A (j \\/ ~ i)) #{j = 1} x))
          (f (trans (\\i. A (~ i)) x))
      def transPiEq (A : I -> U) (B : Pi (i : I) -> A i -> U)
          : Eq ((Pi (x : A 0) -> B 0 x) -> (Pi (x : A 1) -> B 1 x))
               (transPi A B)
               (trans (\\i. Pi (x : A i) -> B i x))
          => \\i. transPi A B
      def transSigma (A : I -> U) (B : Pi (i : I) -> A i -> U)
          (t : Sig (x : A 0) ** B 0 x) : Sig (x : A 1) ** B 1 x =>
        << trans A (t.1),
           trans (\\j. B j (tr (\\i. A (j /\\ i)) #{j = 0} (t.1))) (t.2) >>
      def transSigmaEq (A : I -> U) (B : Pi (i : I) -> A i -> U)
          : Eq ((Sig (x : A 0) ** B 0 x) -> (Sig (x : A 1) ** B 1 x))
               (transSigma A B)
               (trans (\\i. Sig (x : A i) ** B i x))
          => \\i. transSigma A B

      def subst (A : Type) (P : A -> Type) (p : I -> A)
                (lhs : P (p 0)) : P (p 1) =>
        trans (\\i. P (p i)) lhs
      def =-trans (A : Type) (p : I -> A) (q : [| i |] A {| i = 0 |-> p 1 |})
          : [| i |] A {| i = 0 |-> p 0 | i = 1 |-> q 1 |} =>
        subst A (Eq A (p 0)) (\\i. q i) (\\i. p i)

      def transId (A : U) (a : A) : Eq A a (tr (\\i. A) #{1=1} a) => \\i. a
      def forward (A : I -> Type) (r : I) : A r -> A 1 =>
        tr (\\i. A (r \\/ i)) #{r = 1}
      """);
  }

  @Test public void partial() {
    tyck("""
      def par1 (A : Type) (u : A) (i : I) : Partial A #{i = 0} =>
        \\ {| i = 0 |-> u |}
      def par2 (A : Type) (u : A) (i : I) : Partial A #{i = 0} =>
        \\ {| i = 0 |-> u | i = 1 |-> u |}
      def par3 (A : Type) (u : A) (v : A) (i : I) : Partial A #{i = 0 \\/ i = 1} =>
        \\ {| i = 0 |-> u | i = 1 |-> v |}
      def par4 (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 \\/ i = 1 /\\ j = 0} =>
        \\ {| i = 0 |-> u | i = 1 /\\ j = 0 |-> v |}
      def par5 (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 \\/ i = 1 /\\ j = 0} =>
        \\ {| i = 0 |-> u | i = 1 |-> v |}
            """);
  }

  @Test public void partialTooRestricted() {
    assertThrowsExactly(SPE.class, () -> tyck("""
      def par (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 \\/ i = 1} =>
        \\ {| i = 0 |-> u | i = 1 /\\ j = 0 |-> v |}
      """));
  }

  @Test public void partialDisagree() {
    assertThrowsExactly(SPE.class, () -> tyck("""
      def par (A : Type) (u : A) (v : A) (i : I) (j : I) : Partial A #{i = 0 /\\ j = 0} =>
        \\ {| i = 0 |-> u | i = 0 /\\ j = 0 |-> v |}
      """));
  }

  private static @NotNull Elaborator tyck(@Language("TEXT") String s) {
    return CliMain.tyck(s, false);
  }
}
