package org.aya.guest0x0.tyck;

import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.CompData;
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
  record Transps(@NotNull Term cover, @NotNull Term.Cof restr) {
    public @NotNull Term inv() {
      return new Transp(mkLam("i", i -> cover.app(neg(i))), restr);
    }

    /** Marisa Kirisame!! */
    public @NotNull Term mk() {return new Transp(cover, restr);}

    public @NotNull Term fill(@NotNull LocalVar i) {
      var ri = new Ref(i);
      return new Transp(mkLam("j", j -> cover.app(and(ri, j))),
        new Cof(restr.restr().or(new Restr.Cond<>(ri, true))));
    }

    public @NotNull Term invFill(@NotNull LocalVar i) {
      var ri = new Ref(i);
      return new Transp(mkLam("j", j -> cover.app(neg(and(neg(ri), j)))),
        new Cof(restr.restr().or(new Restr.Cond<>(ri, false))));
    }
  }
  static @NotNull Term forward(@NotNull Term cover, @NotNull Term r) {
    return new Transp(mkLam("i", i -> cover.app(or(i, r))),
      new Cof(Restr.fromCond(new Restr.Cond<>(r, false))));
  }
  /**
   * CCHM comp, similar to the analogous comp as in
   * <a href="https://github.com/molikto/mlang/blob/5110e18d20484a3f4ee57ee68e2793e5cf0e28e6/src-main/src/main/scala/mlang/compiler/semantic/10_value_fibrant.scala#L319-L324">mlang</a>
   *
   * @param x   the wall dimension
   * @param par has access to <code>x</code>
   */
  static @NotNull Term comp(@NotNull Term cover, @NotNull LocalVar x, @NotNull PartEl par, @NotNull Term u0) {
    assert !(par instanceof SomewhatPartial);
    return new Hcomp(new CompData<>(new Cof(par.restr()),
      cover.app(end(false)), new Lam(x, par.fmap(u -> forward(cover, new Ref(x)).app(u))), u0));
  }
}
