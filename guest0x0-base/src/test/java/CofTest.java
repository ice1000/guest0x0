import kala.collection.Seq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import kala.tuple.Tuple2;
import org.aya.guest0x0.cli.CliMain;
import org.aya.guest0x0.cli.Parser;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.Elaborator;
import org.aya.guest0x0.tyck.Normalizer;
import org.aya.guest0x0.tyck.Resolver;
import org.aya.guest0x0.util.LocalVar;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourceFile;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CofTest {
  public Restr<Term> substCof(@Language("TEXT") String s, String i, @Language("TEXT") String to, String... vars) {
    var context = context(vars);
    var parser = new Parser(SourceFile.NONE);
    var raw = parser.restr(CliMain.parser(s).psi()).fmap(context._1::expr);
    var cof = raw.mapCond(c -> new Restr.Cond<>(context._2.inherit(c.inst(), Term.I), c.isLeft()));
    var tot = context._2.inherit(context._1.expr(parser.expr(CliMain.parser(to).expr())), Term.I);
    var subst = new Normalizer(context._2.sigma(), MutableMap.of(context._1.env().get(i), tot));
    return subst.restr(cof);
  }

  public @NotNull Tuple2<Resolver, Elaborator> context(String[] vars) {
    var resolver = new Resolver(MutableMap.from(Seq.from(vars).view().map(v -> Tuple.of(v, new LocalVar(v)))));
    var akJr = CliMain.andrasKovacs();
    resolver.env().forEach((k, v) -> akJr.gamma().put(v, Term.I));
    return Tuple.of(resolver, akJr);
  }

  private static void assertDoc(@Language("TEXT") String expected, Docile actual) {
    assertEquals(expected, actual.toDoc().renderWithPageWidth(80, false));
  }

  @Test public void simpleSubst() {
    var cof = substCof("i = 0 \\/ j = 1", "i", "k", "i", "j", "k");
    assertDoc("k = 0 \\/ j = 1", cof);
  }
}
