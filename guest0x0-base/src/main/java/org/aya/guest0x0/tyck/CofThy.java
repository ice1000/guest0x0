package org.aya.guest0x0.tyck;

import org.aya.guest0x0.syntax.Formula;
import org.aya.guest0x0.syntax.Restr;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/** This is a reference to cooltt's codebase */
public record CofThy(@NotNull Restr<Term> restriction) {
  public boolean propExt(@NotNull Normalizer initial, @NotNull Predicate<Normalizer> sat) {
    return switch (restriction) {
      case Restr.Const c -> !c.isTrue() || sat.test(initial);
      case Restr.Vary<Term> restr -> {
        for (var or : restr.orz()) {
          var derived = initial.derive();
          var unsat = false;
          for (var eq : or.ands()) {
            if (eq.inst().asFormula() instanceof Formula.Lit<?> lit && lit.isLeft() != eq.isLeft())
              unsat = true;
            else if (eq.inst() instanceof Term.Ref ref)
              derived.rho().put(ref.var(), Term.end(eq.isLeft()));
            else yield false;
          }
          if (unsat) continue; // Skip unsatisfiable cases
          if (!sat.test(derived)) yield false;
        }
        yield true;
      }
    };
  }

  public boolean satisfied() {
    return switch (restriction) {
      case Restr.Const c -> c.isTrue();
      case Restr.Vary<Term> restr -> {
        for (var or : restr.orz()) {
          var satisfied = true;
          for (var eq : or.ands()) {
            var matchy = eq.inst().asFormula() instanceof Formula.Lit<?> lit
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
