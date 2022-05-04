package org.aya.guest0x0.syntax;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableStack;
import org.aya.guest0x0.cubical.Formula;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public sealed interface Restr<E extends Restr.TermLike<E>> {
  interface TermLike<E extends TermLike<E>> {
    default @Nullable Formula<E> asFormula() {return null;}
  }
  /** I'm sorry, I'm just too bad at writing while loops */
  static <T> void combineRecursively(
    SeqView<Formula.Conn<T>> localOrz,
    MutableStack<Cond<T>> conds,
    MutableList<Cofib<T>> combined
  ) {
    if (localOrz.isEmpty()) {
      combined.append(new Cofib<>(conds.toImmutableArray()));
      return;
    }
    var conn = localOrz.first();
    var lateDropped = localOrz.drop(1);
    if (conn.isAnd()) { // a /\ b = 0 ==> a = 0 \/ b = 0
      conds.push(new Cond<>(conn.l(), true));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Cond<>(conn.r(), true));
    } else { // a \/ b = 1 ==> a = 1 \/ b = 1
      conds.push(new Cond<>(conn.l(), false));
      combineRecursively(lateDropped, conds, combined);
      conds.pop();
      conds.push(new Cond<>(conn.r(), false));
    }
    combineRecursively(lateDropped, conds, combined);
    conds.pop();
  }
  /**
   * Normalizes a "restriction" which looks like "f1 \/ f2 \/ ..." where
   * f1, f2 are like "a /\ b /\ ...".
   */
  static <E extends TermLike<E>> @NotNull Restr<E> normalizeRestr(Vary<E> vary) {
    var orz = MutableArrayList.<Cofib<E>>create(vary.orz().size());
    // This is a sequence of "or"s, so if any cof is true, the whole thing is true
    for (var cof : vary.orz()) if (normalizeCof(cof, orz)) return new Const<>(true);
    if (orz.isEmpty()) return new Const<>(false);
    return new Vary<>(orz.toImmutableArray());
  }
  /**
   * Only when we cannot simplify an LHS do we add it to "ands".
   * Unsimplifiable terms are basically non-formulae (e.g. variable references, neutrals, etc.)
   * In case of \/, we add them to "orz" and do not add to "ands".
   *
   * @return true if this is constant false
   */
  static <E extends TermLike<E>> boolean collectAnds(
    Cofib<E> cof,
    MutableList<Cond<E>> ands,
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
        case Formula.Inv<E> inv -> todoAnds.push(new Cond<>(inv.i(), !and.isLeft()));
        // a /\ b = 1 ==> a = 1 /\ b = 1
        case Formula.Conn<E> conn && conn.isAnd() && !and.isLeft() -> {
          todoAnds.push(new Cond<>(conn.l(), false));
          todoAnds.push(new Cond<>(conn.r(), false));
        }
        // a \/ b = 0 ==> a = 0 /\ b = 0
        case Formula.Conn<E> conn && !conn.isAnd() && and.isLeft() -> {
          todoAnds.push(new Cond<>(conn.l(), true));
          todoAnds.push(new Cond<>(conn.r(), true));
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
  static <E extends TermLike<E>> boolean normalizeCof(Cofib<E> cof, MutableList<Cofib<E>> orz) {
    var ands = MutableArrayList.<Cond<E>>create(cof.ands().size());
    var localOrz = MutableList.<Formula.Conn<E>>create();
    // If a false is found, do not modify orz
    if (collectAnds(cof, ands, localOrz)) return false;
    if (localOrz.isNotEmpty()) {
      var combined = MutableArrayList.<Cofib<E>>create(1 << localOrz.size());
      combineRecursively(localOrz.view(), ands.asMutableStack(), combined);
      // `cofib` has side effects, so you must first traverse them and then call `allMatch`
      // Can I do this without recursion?
      return combined.map(cofib -> normalizeCof(cofib, orz)).allMatch(b -> b);
    }
    if (ands.isNotEmpty()) {
      orz.append(new Cofib<>(ands.toImmutableArray()));
      return false;
    } else return true;
  }
  Restr<E> fmap(@NotNull Function<E, E> g);
  Restr<E> or(Cond<E> cond);
  <T extends TermLike<T>> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f);
  record Vary<E extends TermLike<E>>(@NotNull ImmutableSeq<Cofib<E>> orz) implements Restr<E> {
    @Override public Vary<E> fmap(@NotNull Function<E, E> g) {
      return new Vary<>(orz.map(x -> x.rename(g)));
    }

    @Override public Vary<E> or(Cond<E> cond) {
      return new Vary<>(orz.appended(new Cofib<>(ImmutableSeq.of(cond))));
    }

    @Override public <T extends TermLike<T>> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Vary<>(orz.map(x -> new Cofib<>(x.ands.map(f))));
    }
  }
  record Const<E extends TermLike<E>>(boolean isTrue) implements Restr<E> {
    @Override public Const<E> fmap(@NotNull Function<E, E> g) {
      return this;
    }

    @Override public Restr<E> or(Cond<E> cond) {
      return isTrue ? this : new Vary<>(ImmutableSeq.of(new Cofib<>(ImmutableSeq.of(cond))));
    }

    @Override public <T extends TermLike<T>> Const<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Const<>(isTrue);
    }
  }
  record Cond<E>(@NotNull E inst, boolean isLeft) {
    public Cond<E> rename(@NotNull Function<E, E> g) {
      return new Cond<>(g.apply(inst), isLeft);
    }
  }
  record Cofib<E>(@NotNull ImmutableSeq<Cond<E>> ands) {
    public Cofib<E> rename(@NotNull Function<E, E> g) {
      return new Cofib<>(ands.map(c -> c.rename(g)));
    }
  }
}
