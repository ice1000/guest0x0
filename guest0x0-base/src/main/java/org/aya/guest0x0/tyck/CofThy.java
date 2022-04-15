package org.aya.guest0x0.tyck;

import org.aya.guest0x0.syntax.Formula;
import org.aya.guest0x0.syntax.Restr;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

/** This is a reference to cooltt's codebase */
public interface CofThy {
  static boolean satisfied(@NotNull Restr<Term> restriction) {
    return switch (restriction) {
      case Restr.Const c -> c.isTrue();
      case Restr.Vary<Term> restr -> {
        for (var or : restr.orz()) {
          var satisfied = true;
          for (var eq : or.ands()) {
            var matchy = eq.inst() instanceof Term.Mula mula
              && mula.formula() instanceof Formula.Lit<?> lit
              && lit.isLeft() == eq.isLeft();
            satisfied = satisfied && matchy;
          }
          if (satisfied) yield true;
        }
        yield false;
      }
    };
  }
}
