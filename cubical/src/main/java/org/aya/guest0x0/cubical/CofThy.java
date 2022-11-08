// Copyright (c) 2020-2022 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
package org.aya.guest0x0.cubical;

import kala.control.Option;
import org.aya.guest0x0.cubical.Restr.TermLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Operations on face restrictions (cofibrations in cartesian cubical type theory),
 * for normalization, simplification, satisfaction, etc.
 *
 * @see RestrSimplifier
 */
public interface CofThy {
  @FunctionalInterface
  interface RestrNormalizer<E extends TermLike<E>, V, Subst extends SubstObj<E, V, Subst>>
    extends BiFunction<Subst, Restr<E>, Restr<E>> {
  }

  static <E extends TermLike<E>, V, Subst extends SubstObj<E, V, Subst>> boolean
  propExt(Subst subst, Restr<E> ll, Restr<E> rr, RestrNormalizer<E, V, Subst> normalize) {
    return conv(ll, subst, sub -> satisfied(normalize.apply(sub, rr)))
      && conv(rr, subst, sub -> satisfied(normalize.apply(sub, ll)));
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
      if (inst.asFormula() instanceof Formula.Lit<?> lit && !lit.isOne() == eq.isOne())
        unsat = true;
      else {
        var castVar = initial.asRef(inst);
        if (castVar != null) {
          if (derived.contradicts(castVar, eq.isOne()))
            return Option.none(); // Unsatisfiable
          else derived.put(castVar, eq.isOne());
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
    /** Put <code>i := I(isOne)</code> into the substitution */
    void put(V i, boolean isOne);
    /** @return true if there is <code>i := I(oldIsOne)</code> while <code>oldIsOne != newIsOne</code> */
    boolean contradicts(V i, boolean newIsOne);
    @Nullable V asRef(@NotNull E term);
    @NotNull Subst derive();
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
            else if (!lit.isOne() == eq.isOne())
              satisfied = false;
          }
          if (satisfied) yield true;
        }
        yield false;
      }
    };
  }
}
