package org.aya.guest0x0.tyck;

import org.aya.guest0x0.syntax.Restr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import static org.aya.guest0x0.syntax.Term.*;

/**
 * References:
 * <ul>
 * <li><a href="https://github.com/molikto/mlang/blob/5110e18d20484a3f4ee57ee68e2793e5cf0e28e6/src-main/src/main/scala/mlang/compiler/semantic/10_value_fibrant.scala">10_value_fibrant.scala</a></li>
 * <li><a href="https://www.cse.chalmers.se/~simonhu/misc/hcomp.pdf">hcomp.pdf</a></li>
 * </ul>
 */
public interface HCompPDF {
  record Transps(@NotNull Term cover, @NotNull Restr<Term> restr) {
    public @NotNull Term inv() {
      return new Transp(mkLam("i", i -> cover.app(neg(i))), restr);
    }

    /** Marisa Kirisame!! */
    public @NotNull Term mk() {return new Transp(cover, restr);}

    public @NotNull Term fill(@NotNull LocalVar i) {
      var ri = new Ref(i);
      return new Transp(mkLam("j", j -> cover.app(and(ri, j))), restr.or(new Restr.Cond<>(ri, true)));
    }

    public @NotNull Term invFill(@NotNull LocalVar i) {
      var ri = new Ref(i);
      return new Transp(mkLam("j", j -> cover.app(neg(and(neg(ri), j)))), restr.or(new Restr.Cond<>(ri, false)));
    }
  }
}
