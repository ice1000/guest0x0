package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Operations on face restrictions (cofibrations in cartesian cubical type theory),
 * for normalization, simplification, satisfaction, etc.
 */
public interface CofThy {
  /** I'm sorry, I'm just too bad at writing while loops */
  static <T> void combineRecursively(
    SeqView<Formula.Conn<T>> localOrz,
    MutableStack<Restr.Cond<T>> conds,
    MutableList<Restr.Cofib<T>> combined
  ) {
    if (localOrz.isEmpty()) {
      combined.append(new Restr.Cofib<>(conds.toImmutableArray()));
      return;
    }
    var conn = localOrz.first();
    var lateDropped = localOrz.drop(1);
    if (conn.isAnd()) { // a /\ b = 0 ==> a = 0 \/ b = 0
      conds.push(new Restr.Cond<>(conn.l(), true));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Restr.Cond<>(conn.r(), true));
    } else { // a \/ b = 1 ==> a = 1 \/ b = 1
      conds.push(new Restr.Cond<>(conn.l(), false));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Restr.Cond<>(conn.r(), false));
    }
    combineRecursively(lateDropped, conds, combined);
    conds.pop();
  }

  /**
   * Equality-checking (any procedure that returns a boolean) under a cofibration.
   *
   * @return true if the cofibration is false, <code>sat.test(subst)</code> otherwise
   * @see SubstObj
   */
  static <E extends Restr.TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  vdash(@NotNull Restr<E> r, @NotNull Subst initial, @NotNull Predicate<Subst> sat) {
    return switch (r) {
      case Restr.Const c -> !c.isTrue() || sat.test(initial);
      case Restr.Vary<E> restr -> {
        for (var or : restr.orz()) {
          var derived = initial.derive();
          var unsat = false;
          for (var eq : or.ands()) {
            if (eq.inst().asFormula() instanceof Formula.Lit<?> lit && lit.isLeft() != eq.isLeft())
              unsat = true;
            else {
              var castVar = initial.asRef(eq.inst());
              if (castVar != null) {
                derived.put(castVar, eq.isLeft());
              } else yield false;
            }
          }
          if (unsat) continue; // Skip unsatisfiable cases
          if (!sat.test(derived)) yield false;
        }
        yield true;
      }
    };
  }

  /**
   * Representation of a generic <strong>interval</strong> substitution object.
   *
   * @param <E> "terms"
   * @param <V> "variables" -- assuming capture-avoiding substitution instead of indices
   */
  interface SubstObj<E, V, Subst extends SubstObj<E, V, Subst>> {
    void put(V var, boolean isLeft);
    @Nullable V asRef(@NotNull E term);
    @NotNull Subst derive();
  }

  /**
   * Normalizes a "restriction" which looks like "f1 \/ f2 \/ ..." where
   * f1, f2 are like "a /\ b /\ ...".
   */
  static <E extends Restr.TermLike<E>> @NotNull Restr<E> normalizeRestr(Restr.Vary<E> vary) {
    var orz = MutableArrayList.<Restr.Cofib<E>>create(vary.orz().size());
    // This is a sequence of "or"s, so if any cof is true, the whole thing is true
    for (var cof : vary.orz()) if (normalizeCof(cof, orz)) return new Restr.Const<>(true);
    if (orz.isEmpty()) return new Restr.Const<>(false);
    return new Restr.Vary<>(orz.toImmutableArray());
  }

  /**
   * Only when we cannot simplify an LHS do we add it to "ands".
   * Unsimplifiable terms are basically non-formulae (e.g. variable references, neutrals, etc.)
   * In case of \/, we add them to "orz" and do not add to "ands".
   *
   * @return true if this is constant false
   */
  static <E extends Restr.TermLike<E>> boolean collectAnds(
    Restr.Cofib<E> cof,
    MutableList<Restr.Cond<E>> ands,
    MutableList<Formula.Conn<E>> orz
  ) {
    var todoAnds = MutableList.from(cof.ands()).asMutableStack();
    while (todoAnds.isNotEmpty()) {
      var and = todoAnds.pop();
      switch (and.inst().asFormula()) {
        case Formula.Lit<E> lit -> {
          if (lit.isLeft() != and.isLeft()) return true;
          // Skip truth
        }
        // ~ a = j ==> a = ~ j for j \in {0, 1}
        // According to CCHM, the canonical map takes (1-i) to (i=0)
        case Formula.Inv<E> inv -> todoAnds.push(new Restr.Cond<>(inv.i(), !and.isLeft()));
        // a /\ b = 1 ==> a = 1 /\ b = 1
        case Formula.Conn<E> conn && conn.isAnd() && !and.isLeft() -> {
          todoAnds.push(new Restr.Cond<>(conn.l(), false));
          todoAnds.push(new Restr.Cond<>(conn.r(), false));
        }
        // a \/ b = 0 ==> a = 0 /\ b = 0
        case Formula.Conn<E> conn && !conn.isAnd() && and.isLeft() -> {
          todoAnds.push(new Restr.Cond<>(conn.l(), true));
          todoAnds.push(new Restr.Cond<>(conn.r(), true));
        }
        // a /\ b = 0 ==> a = 0 \/ b = 0
        case Formula.Conn<E> conn && conn.isAnd() /*&& and.isLeft()*/ -> orz.append(conn);
        // a \/ b = 1 ==> a = 1 \/ b = 1
        case Formula.Conn<E> conn /*&& !conn.isAnd() && !and.isLeft()*/ -> orz.append(conn);
        case null -> ands.append(and);
      }
    }
    return false;
  }

  /**
   * Normalizes a list of "a /\ b /\ ..." into orz.
   * If it is false (implied by any of them being false), orz is unmodified.
   *
   * @return true if this is constantly true
   */
  static <E extends Restr.TermLike<E>> boolean normalizeCof(Restr.Cofib<E> cof, MutableList<Restr.Cofib<E>> orz) {
    var ands = MutableArrayList.<Restr.Cond<E>>create(cof.ands().size());
    var localOrz = MutableList.<Formula.Conn<E>>create();
    // If a false is found, do not modify orz
    if (collectAnds(cof, ands, localOrz)) return false;
    if (localOrz.isNotEmpty()) {
      var combined = MutableArrayList.<Restr.Cofib<E>>create(1 << localOrz.size());
      combineRecursively(localOrz.view(), ands.asMutableStack(), combined);
      // `cofib` has side effects, so you must first traverse them and then call `allMatch`
      // Can I do this without recursion?
      return combined.map(cofib -> normalizeCof(cofib, orz)).allMatch(b -> b);
    }
    if (ands.isNotEmpty()) {
      orz.append(new Restr.Cofib<>(ands.toImmutableArray()));
      return false;
    } else return true;
  }

  static boolean satisfied(@NotNull Restr<?> restriction) {
    return switch (restriction) {
      case Restr.Const c -> c.isTrue();
      case Restr.Vary<?> restr -> {
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
