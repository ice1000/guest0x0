import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.control.Either;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.aya.guest0x0.parser.Guest0x0Lexer;
import org.aya.guest0x0.parser.Guest0x0Parser;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Parser;
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
    var artifact = def("def uncurry (A : Type) (B : Type) (C : Type)" +
      "(t : A ** B) (f : A -> B -> C) : C => f (t.1) (t.2)").first();
    var tycked = andrasKovacs().def(artifact);
    assertNotNull(tycked);
  }

  @Test public void dontSayLazy() {
    var akJr = tyck("""
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
    var akJr = tyck("""
      def Eq (A : Type) (a : A) (b : A) : Type => Pi (P : A -> Type) -> P a -> P b
      def refl (A : Type) (a : A) : Eq A a a => \\P. \\pa. pa
      def sym (A : Type) (a : A) (b : A) (e : Eq A a b) : Eq A b a => e (\\b. Eq A b a) (refl A a)
      """);
    assertEquals(3, akJr.sigma().size());
  }

  private @NotNull Elaborator tyck(String code) {
    var artifact = def(code);
    var akJr = andrasKovacs();
    for (var def : artifact) {
      var tycked = akJr.def(def);
      akJr.sigma().put(tycked.name(), tycked);
    }
    return akJr;
  }

  private @NotNull Term tyckExpr(String term, String type) {
    var akJr = andrasKovacs();
    var Id = akJr.synth(expr(type));
    assertEquals(1, akJr.gamma().size());
    return akJr.inherit(expr(term), Id.wellTyped());
  }

  private @NotNull Elaborator andrasKovacs() {
    return new Elaborator(MutableMap.create(), MutableMap.create());
  }

  private @NotNull Expr expr(String s) {
    return new Resolver(MutableMap.create())
      .expr(new Parser(Either.left(SourceFile.NONE)).expr(parser(s).expr()));
  }

  private @NotNull ImmutableSeq<Def<Expr>> def(String s) {
    var decls = ImmutableSeq.from(parser(s).program().decl());
    var edj = new Resolver(MutableMap.create());
    return decls.map(d -> edj.def(new Parser(Either.left(SourceFile.NONE)).def(d)));
  }

  private Guest0x0Parser parser(String s) {
    return new Guest0x0Parser(new CommonTokenStream(new Guest0x0Lexer(CharStreams.fromString(s))));
  }
}
