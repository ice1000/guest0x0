package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface RestrSimplifier<T extends Restr.TermLike<T>, LocalVar> {
  @Nullable LocalVar asRef(T term);
  record CondCollector<T extends Restr.TermLike<T>>(
    MutableStack<Restr.Cond<T>> conds
  ) {
    public Restr.Conj<T> toConj() {
      var arr = MutableArrayList.<Restr.Cond<T>>create(conds.size());

      return new Restr.Conj<>(conds.toImmutableArray());
    }

    @SafeVarargs public final void withCond(@NotNull Runnable code, @NotNull Restr.Cond<T> @NotNull ... more) {
      for (var cond : more) {
        conds.push(cond);
        code.run();
        conds.pop();
      }
    }
  }
  /**
   * I'm sorry, I'm just too bad at writing while loops.
   * Add <code>localOrz</code> into <code>conds</code>, and push the results into <code>combined</code>.
   */
  default void combineRecursively(
    @NotNull SeqView<Formula.Conn<T>> localOrz,
    CondCollector<T> conds,
    MutableList<Restr.Conj<T>> combined
  ) {
    if (localOrz.isEmpty()) {
      combined.append(conds.toConj());
      return;
    }
    var conn = localOrz.first();
    var lateDropped = localOrz.drop(1);
    // a /\ b = 0 ==> a = 0 \/ b = 0
    if (conn.isAnd()) conds.withCond(() -> combineRecursively(lateDropped, conds, combined),
      new Restr.Cond<>(conn.l(), false),
      new Restr.Cond<>(conn.r(), false)
    );
      // a \/ b = 1 ==> a = 1 \/ b = 1
    else conds.withCond(() -> combineRecursively(lateDropped, conds, combined),
      new Restr.Cond<>(conn.l(), true),
      new Restr.Cond<>(conn.r(), true)
    );
  }

  /**
   * Only when we cannot simplify an LHS do we add it to "ands".
   * Unsimplifiable terms are basically non-formulae (e.g. variable references, neutrals, etc.)
   * In case of \/, we add them to "orz" and do not add to "ands".
   *
   * @return true if this is constant false
   */
  default boolean collectAnds(
    Restr.Conj<T> cof,
    MutableList<Restr.Cond<T>> ands,
    MutableList<Formula.Conn<T>> orz
  ) {
    var todoAnds = MutableList.from(cof.ands()).asMutableStack();
    while (todoAnds.isNotEmpty()) {
      var and = todoAnds.pop();
      switch (and.inst().asFormula()) {
        case Formula.Lit<T> lit -> {
          if (!lit.isOne() == and.isOne()) return true;
          // Skip truth
        }
        // ~ a = j ==> a = ~ j for j \in {0, 1}
        // According to CCHM, the canonical map takes (1-i) to (i=0)
        case Formula.Inv<T> inv -> todoAnds.push(new Restr.Cond<>(inv.i(), !and.isOne()));
        // a /\ b = 1 ==> a = 1 /\ b = 1
        case Formula.Conn<T> conn && conn.isAnd() && and.isOne() -> {
          todoAnds.push(new Restr.Cond<>(conn.l(), true));
          todoAnds.push(new Restr.Cond<>(conn.r(), true));
        }
        // a \/ b = 0 ==> a = 0 /\ b = 0
        case Formula.Conn<T> conn && !conn.isAnd() && !and.isOne() -> {
          todoAnds.push(new Restr.Cond<>(conn.l(), false));
          todoAnds.push(new Restr.Cond<>(conn.r(), false));
        }
        // a /\ b = 0 ==> a = 0 \/ b = 0
        case Formula.Conn<T> conn && conn.isAnd() /*&& and.isLeft()*/ -> orz.append(conn);
        // a \/ b = 1 ==> a = 1 \/ b = 1
        case Formula.Conn<T> conn /*&& !conn.isAnd() && !and.isLeft()*/ -> orz.append(conn);
        case null -> ands.append(and);
      }
    }
    return false;
  }

  default @NotNull Partial<T> mapSplit(@NotNull Partial.Split<T> split, @NotNull UnaryOperator<T> mapper) {
    var cl = MutableArrayList.<Restr.Side<T>>create();
    for (var clause : split.clauses()) {
      var u = mapper.apply(clause.u());
      if (normalizeCof(clause.cof().map(mapper), cl, cofib -> new Restr.Side<>(cofib, u))) {
        return new Partial.Const<>(u);
      }
    }
    return new Partial.Split<>(cl.toImmutableArray());
  }


  /**
   * Normalizes a list of "a /\ b /\ ..." into orz.
   * If it is false (implied by any of them being false), orz is unmodified.
   *
   * @return true if this is constantly true
   */
  default <Clause> boolean normalizeCof(
    Restr.Conj<T> cof, MutableList<Clause> orz,
    Function<Restr.Conj<T>, Clause> clause
  ) {
    var ands = MutableArrayList.<Restr.Cond<T>>create(cof.ands().size());
    var localOrz = MutableList.<Formula.Conn<T>>create();
    // If a false is found, do not modify orz
    if (collectAnds(cof, ands, localOrz)) return false;
    if (localOrz.isNotEmpty()) {
      var combined = MutableArrayList.<Restr.Conj<T>>create(1 << localOrz.size());
      combineRecursively(localOrz.view(), new CondCollector<>(ands.asMutableStack()), combined);
      // `cofib` has side effects, so you must first traverse them and then call `allMatch`
      // Can I do this without recursion?
      return combined.map(cofib -> normalizeCof(cofib, orz, clause)).allMatch(b -> b);
    }
    if (ands.isNotEmpty()) {
      orz.append(clause.apply(new Restr.Conj<>(ands.toImmutableArray())));
      return false;
    } else return true;
  }

  /**
   * Normalizes a "restriction" which looks like "f1 \/ f2 \/ ..." where
   * f1, f2 are like "a /\ b /\ ...".
   */
  default @NotNull Restr<T> normalizeRestr(Restr.Disj<T> disj) {
    var orz = MutableArrayList.<Restr.Conj<T>>create(disj.orz().size());
    // This is a sequence of "or"s, so if any cof is true, the whole thing is true
    for (var cof : disj.orz())
      if (normalizeCof(cof, orz, Function.identity()))
        return new Restr.Const<>(true);
    if (orz.isEmpty()) return new Restr.Const<>(false);
    return new Restr.Disj<>(orz.toImmutableArray());
  }


  /** @see RestrSimplifier#isOne(Restr.TermLike) */
  @ApiStatus.Internal default Restr.Disj<T> embed(T e) {
    var conds = ImmutableSeq.of(new Restr.Cond<>(e, true));
    return new Restr.Disj<>(new Restr.Conj<>(conds));
  }

  default Restr<T> isOne(T e) {
    return normalizeRestr(embed(e));
  }
}