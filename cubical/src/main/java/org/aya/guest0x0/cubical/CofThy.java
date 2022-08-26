package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableStack;
import kala.control.Option;
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
  static <T extends Restr.TermLike<T>> void combineRecursively(
    @NotNull SeqView<Formula.Conn<T>> localOrz,
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
  // https://github.com/mortberg/cubicaltt/blob/a5c6f94bfc0da84e214641e0b87aa9649ea114ea/Connections.hs#L178-L197
  static <T extends Restr.TermLike<T>> T formulae(Formula<T> formula, Restr.TermLike.Factory<T> factory) {
    return switch (formula) { // de Morgan laws
      // ~ 1 = 0, ~ 0 = 1
      case Formula.Inv<T> inv && inv.i().asFormula() instanceof Formula.Lit<T> lit ->
        factory.apply(new Formula.Lit<>(!lit.isLeft()));
      // ~ (~ a) = a
      case Formula.Inv<T> inv && inv.i().asFormula() instanceof Formula.Inv<T> ii -> ii.i(); // DNE!! :fear:
      // ~ (a /\ b) = (~ a \/ ~ b), ~ (a \/ b) = (~ a /\ ~ b)
      case Formula.Inv<T> inv && inv.i().asFormula() instanceof Formula.Conn<T> conn ->
        factory.apply(new Formula.Conn<>(!conn.isAnd(),
          formulae(new Formula.Inv<>(conn.l()), factory),
          formulae(new Formula.Inv<>(conn.r()), factory)));
      // 0 /\ a = 0, 1 /\ a = a, 0 \/ a = a, 1 \/ a = 1
      case Formula.Conn<T> conn && conn.l().asFormula() instanceof Formula.Lit<T> l -> l.isLeft()
        ? (conn.isAnd() ? conn.l() : conn.r())
        : (conn.isAnd() ? conn.r() : conn.l());
      // a /\ 0 = 0, a /\ 1 = a, a \/ 0 = a, a \/ 1 = 1
      case Formula.Conn<T> conn && conn.r().asFormula() instanceof Formula.Lit<T> r -> r.isLeft()
        ? (conn.isAnd() ? conn.r() : conn.l())
        : (conn.isAnd() ? conn.l() : conn.r());
      default -> factory.apply(formula);
    };
  }

  @FunctionalInterface
  interface RestrNormalizer<E extends Restr.TermLike<E>, V, Subst extends SubstObj<E, V, Subst>>
    extends BiFunction<Subst, Restr<E>, Restr<E>> {
  }

  static <E extends Restr.TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  propExt(Subst subst, Restr<E> ll, Restr<E> rr, RestrNormalizer<E, V, Subst> normalize) {
    return conv(ll, subst, sub -> satisfied(normalize.apply(sub, rr)))
      && conv(rr, subst, sub -> satisfied(normalize.apply(sub, ll)));
  }

  /** @see CofThy#conv(Restr, SubstObj, Predicate) */
  static <E extends Restr.TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  conv(@NotNull Restr.Cofib<E> r, @NotNull Subst initial, @NotNull Predicate<Subst> sat) {
    return conv(new Restr.Vary<>(ImmutableSeq.of(r)), initial, sat);
  }

  /**
   * Equality-checking (any procedure that returns a boolean) under a cofibration.
   *
   * @return true if the cofibration is false, <code>sat.test(subst)</code> otherwise
   * @see SubstObj
   */
  static <E extends Restr.TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  conv(@NotNull Restr<E> r, @NotNull Subst initial, @NotNull Predicate<Subst> sat) {
    return switch (r) {
      case Restr.Const<E> c -> !c.isTrue() || sat.test(initial);
      case Restr.Vary<E> restr -> {
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
  static <V, T extends Restr.TermLike<T>, Subst extends SubstObj<T, V, Subst>, E> Option<E>
  vdash(@NotNull Restr.Cofib<T> or, @NotNull Subst initial, @NotNull Function<Subst, E> tyck) {
    var derived = initial.derive();
    var unsat = false;
    for (var eq : or.ands()) {
      var inst = eq.inst();
      if (inst.asFormula() instanceof Formula.Lit<?> lit && lit.isLeft() != eq.isLeft())
        unsat = true;
      else {
        var castVar = initial.asRef(inst);
        if (castVar != null) {
          if (derived.contradicts(castVar, eq.isLeft()))
            return Option.none(); // Unsatisfiable
          else derived.put(castVar, eq.isLeft());
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
  static <E extends Restr.TermLike<E>> @NotNull Restr<E> normalizeRestr(Restr.Vary<E> vary) {
    var orz = MutableArrayList.<Restr.Cofib<E>>create(vary.orz().size());
    // This is a sequence of "or"s, so if any cof is true, the whole thing is true
    for (var cof : vary.orz())
      if (normalizeCof(cof, orz, Function.identity()))
        return new Restr.Const<>(true);
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
  static <E extends Restr.TermLike<E>, Clause> boolean normalizeCof(
    Restr.Cofib<E> cof, MutableList<Clause> orz,
    Function<Restr.Cofib<E>, Clause> clause
  ) {
    var ands = MutableArrayList.<Restr.Cond<E>>create(cof.ands().size());
    var localOrz = MutableList.<Formula.Conn<E>>create();
    // If a false is found, do not modify orz
    if (collectAnds(cof, ands, localOrz)) return false;
    if (localOrz.isNotEmpty()) {
      var combined = MutableArrayList.<Restr.Cofib<E>>create(1 << localOrz.size());
      combineRecursively(localOrz.view(), ands.asMutableStack(), combined);
      // `cofib` has side effects, so you must first traverse them and then call `allMatch`
      // Can I do this without recursion?
      return combined.map(cofib -> normalizeCof(cofib, orz, clause)).allMatch(b -> b);
    }
    if (ands.isNotEmpty()) {
      orz.append(clause.apply(new Restr.Cofib<>(ands.toImmutableArray())));
      return false;
    } else return true;
  }

  static boolean satisfied(@NotNull Restr<?> restriction) {
    return switch (restriction) {
      case Restr.Const<?> c -> c.isTrue();
      case Restr.Vary<?> restr -> {
        for (var or : restr.orz()) {
          var satisfied = true;
          for (var eq : or.ands()) {
            if (!(eq.inst().asFormula() instanceof Formula.Lit<?> lit))
              satisfied = false;
            else if (lit.isLeft() != eq.isLeft())
              satisfied = false;
          }
          if (satisfied) yield true;
        }
        yield false;
      }
    };
  }
}
