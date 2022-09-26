package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableStack;
import kala.control.Option;
import org.aya.guest0x0.cubical.Restr.TermLike;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Operations on face restrictions (cofibrations in cartesian cubical type theory),
 * for normalization, simplification, satisfaction, etc.
 */
public interface CofThy {
  /**
   * I'm sorry, I'm just too bad at writing while loops.
   * Add <code>localOrz</code> into <code>conds</code>, and push the results into <code>combined</code>.
   */
  static <T extends TermLike<T>> void combineRecursively(
    @NotNull SeqView<Formula.Conn<T>> localOrz,
    MutableStack<Restr.Cond<T>> conds,
    MutableList<Restr.Conj<T>> combined
  ) {
    if (localOrz.isEmpty()) {
      combined.append(new Restr.Conj<>(conds.toImmutableArray()));
      return;
    }
    var conn = localOrz.first();
    var lateDropped = localOrz.drop(1);
    if (conn.isAnd()) { // a /\ b = 0 ==> a = 0 \/ b = 0
      conds.push(new Restr.Cond<>(conn.l(), false));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Restr.Cond<>(conn.r(), false));
    } else { // a \/ b = 1 ==> a = 1 \/ b = 1
      conds.push(new Restr.Cond<>(conn.l(), true));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Restr.Cond<>(conn.r(), true));
    }
    combineRecursively(lateDropped, conds, combined);
    conds.pop();
  }

  @FunctionalInterface
  interface RestrNormalizer<E extends TermLike<E>, V, Subst extends SubstObj<E, V, Subst>>
    extends BiFunction<Subst, Restr<E>, Restr<E>> {
  }

  static <E extends TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  propExt(Subst subst, Restr<E> ll, Restr<E> rr, RestrNormalizer<E, V, Subst> normalize) {
    return conv(ll, subst, sub -> satisfied(normalize.apply(sub, rr)))
      && conv(rr, subst, sub -> satisfied(normalize.apply(sub, ll)));
  }

  /** @see CofThy#isOne(TermLike) */
  @ApiStatus.Internal static <E extends TermLike<E>> Restr.Disj<E> embed(E e) {
    var conds = ImmutableSeq.of(new Restr.Cond<>(e, true));
    return new Restr.Disj<>(ImmutableSeq.of(new Restr.Conj<>(conds)));
  }

  static <E extends TermLike<E>> Restr<E> isOne(E e) {
    return normalizeRestr(embed(e));
  }

  /** @see CofThy#conv(Restr, SubstObj, Predicate) */
  static <E extends TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  conv(@NotNull Restr.Conj<E> r, @NotNull Subst initial, @NotNull Predicate<Subst> sat) {
    return conv(new Restr.Disj<>(r), initial, sat);
  }

  /**
   * Equality-checking (any procedure that returns a boolean) under a cofibration.
   *
   * @return true if the cofibration is false, <code>sat.test(subst)</code> otherwise
   * @see SubstObj
   */
  static <E extends TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  conv(@NotNull Restr<E> r, @NotNull Subst initial, @NotNull Predicate<Subst> sat) {
    return switch (r) {
      case Restr.Const<E> c -> !c.isOne() || sat.test(initial);
      case Restr.Disj<E> restr -> {
        for (var or : restr.orz()) {
          var result = vdash(or, initial, sat::test);
          if (result.isEmpty()) continue; // Skip unsatisfiable cases
          var resBool = result.get();
          if (resBool == null) yield false; // Cofib with nontrivial variables, like (i /\ j) = 0
          if (!resBool) yield false;
        }
        yield true;
      }
    };
  }

  /**
   * Type-checking (any procedure) under an and-only cofibration.
   *
   * @return Ideally it would apply <code>tyck</code> to the body of <code>r</code>, but: <ul>
   * <li><code>Option.none()</code> if the cofib is not satisfiable</li>
   * <li><code>Option.some(null)</code> if the cofib is invalid (cannot be made a substitution)</li>
   * <li><code>Option.some(tyck(subst, u))</code> if everything's fine</li>
   * </ul>
   * @see SubstObj
   */
  static <V, T extends TermLike<T>, Subst extends SubstObj<T, V, Subst>, E> Option<E>
  vdash(@NotNull Restr.Conj<T> or, @NotNull Subst initial, @NotNull Function<Subst, E> tyck) {
    var derived = initial.derive();
    var unsat = false;
    for (var eq : or.ands()) {
      var inst = eq.inst();
      if (inst.asFormula() instanceof Formula.Lit<?> lit && lit.isLeft() == eq.isOne())
        unsat = true;
      else {
        var castVar = initial.asRef(inst);
        if (castVar != null) {
          if (derived.contradicts(castVar, !eq.isOne()))
            return Option.none(); // Unsatisfiable
          else derived.put(castVar, !eq.isOne());
        } else return Option.some(null);
      }
    }
    if (unsat) return Option.none(); // Skip unsatisfiable cases
    return Option.some(tyck.apply(derived));
  }

  /**
   * Representation of a generic <strong>interval</strong> substitution object.
   *
   * @param <E> "terms"
   * @param <V> "variables" -- assuming capture-avoiding substitution instead of indices
   */
  interface SubstObj<E, V, Subst extends SubstObj<E, V, Subst>> {
    /** Put <code>i := I(isLeft)</code> into the substitution */
    void put(V i, boolean isLeft);
    /** @return true if there is <code>i := I(oldIsLeft)</code> while <code>oldIsLeft != newIsLeft</code> */
    boolean contradicts(V i, boolean newIsLeft);
    @Nullable V asRef(@NotNull E term);
    @NotNull Subst derive();
  }

  /**
   * Normalizes a "restriction" which looks like "f1 \/ f2 \/ ..." where
   * f1, f2 are like "a /\ b /\ ...".
   */
  static <E extends TermLike<E>> @NotNull Restr<E> normalizeRestr(Restr.Disj<E> disj) {
    var orz = MutableArrayList.<Restr.Conj<E>>create(disj.orz().size());
    // This is a sequence of "or"s, so if any cof is true, the whole thing is true
    for (var cof : disj.orz())
      if (normalizeCof(cof, orz, Function.identity()))
        return new Restr.Const<>(true);
    if (orz.isEmpty()) return new Restr.Const<>(false);
    return new Restr.Disj<>(orz.toImmutableArray());
  }

  /**
   * Only when we cannot simplify an LHS do we add it to "ands".
   * Unsimplifiable terms are basically non-formulae (e.g. variable references, neutrals, etc.)
   * In case of \/, we add them to "orz" and do not add to "ands".
   *
   * @return true if this is constant false
   */
  static <E extends TermLike<E>> boolean collectAnds(
    Restr.Conj<E> cof,
    MutableList<Restr.Cond<E>> ands,
    MutableList<Formula.Conn<E>> orz
  ) {
    var todoAnds = MutableList.from(cof.ands()).asMutableStack();
    while (todoAnds.isNotEmpty()) {
      var and = todoAnds.pop();
      switch (and.inst().asFormula()) {
        case Formula.Lit<E> lit -> {
          if (lit.isLeft() == and.isOne()) return true;
          // Skip truth
        }
        // ~ a = j ==> a = ~ j for j \in {0, 1}
        // According to CCHM, the canonical map takes (1-i) to (i=0)
        case Formula.Inv<E> inv -> todoAnds.push(new Restr.Cond<>(inv.i(), !and.isOne()));
        // a /\ b = 1 ==> a = 1 /\ b = 1
        case Formula.Conn<E> conn && conn.isAnd() && and.isOne() -> {
          todoAnds.push(new Restr.Cond<>(conn.l(), true));
          todoAnds.push(new Restr.Cond<>(conn.r(), true));
        }
        // a \/ b = 0 ==> a = 0 /\ b = 0
        case Formula.Conn<E> conn && !conn.isAnd() && !and.isOne() -> {
          todoAnds.push(new Restr.Cond<>(conn.l(), false));
          todoAnds.push(new Restr.Cond<>(conn.r(), false));
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
  static <E extends TermLike<E>, Clause> boolean normalizeCof(
    Restr.Conj<E> cof, MutableList<Clause> orz,
    Function<Restr.Conj<E>, Clause> clause
  ) {
    var ands = MutableArrayList.<Restr.Cond<E>>create(cof.ands().size());
    var localOrz = MutableList.<Formula.Conn<E>>create();
    // If a false is found, do not modify orz
    if (collectAnds(cof, ands, localOrz)) return false;
    if (localOrz.isNotEmpty()) {
      var combined = MutableArrayList.<Restr.Conj<E>>create(1 << localOrz.size());
      combineRecursively(localOrz.view(), ands.asMutableStack(), combined);
      // `cofib` has side effects, so you must first traverse them and then call `allMatch`
      // Can I do this without recursion?
      return combined.map(cofib -> normalizeCof(cofib, orz, clause)).allMatch(b -> b);
    }
    if (ands.isNotEmpty()) {
      orz.append(clause.apply(new Restr.Conj<>(ands.toImmutableArray())));
      return false;
    } else return true;
  }

  static boolean satisfied(@NotNull Restr<?> restriction) {
    return switch (restriction) {
      case Restr.Const<?> c -> c.isOne();
      case Restr.Disj<?> restr -> {
        for (var or : restr.orz()) {
          var satisfied = true;
          for (var eq : or.ands()) {
            if (!(eq.inst().asFormula() instanceof Formula.Lit<?> lit))
              satisfied = false;
            else if (lit.isLeft() == eq.isOne())
              satisfied = false;
          }
          if (satisfied) yield true;
        }
        yield false;
      }
    };
  }
}
