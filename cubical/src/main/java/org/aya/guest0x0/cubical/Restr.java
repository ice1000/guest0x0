package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Face restrictions.
 *
 * @param <E> "terms"
 * @see CofThy for cofibration operations
 */
public sealed interface Restr<E extends Restr.TermLike<E>> extends Serializable {
  @NotNull SeqView<E> instView();
  interface TermLike<E extends TermLike<E>> {
    default @Nullable Formula<E> asFormula() {return null;}

    @FunctionalInterface
    interface Factory<T> extends Function<@NotNull Formula<T>, T> {
      @Override @Contract(value = "_->new", pure = true)
      T apply(@NotNull Formula<T> formula);
    }
  }
  @NotNull Restr<E> map(@NotNull Function<E, E> g);
  @NotNull <T extends TermLike<T>> Restr<T>
  fmap(@NotNull Function<E, T> g);
  @NotNull Restr<E> or(@NotNull Cond<E> cond);
  <T extends TermLike<T>> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f);
  record Disj<E extends TermLike<E>>(@NotNull ImmutableSeq<Conj<E>> orz) implements Restr<E> {
    @java.lang.SafeVarargs public Disj(Conj<E>... orz) {
      this(ImmutableSeq.of(orz));
    }

    @Override public @NotNull SeqView<E> instView() {
      return orz.view().flatMap(Conj::view);
    }

    @Override public @NotNull Restr.Disj<E> map(@NotNull Function<E, E> g) {
      var newOrz = orz.map(x -> x.map(g));
      if (newOrz.sameElements(orz, true)) return this;
      return new Disj<>(newOrz);
    }

    @Override public @NotNull <T extends TermLike<T>>
    Restr.Disj<T> fmap(@NotNull Function<E, T> g) {
      return new Disj<>(orz.map(x -> x.fmap(g)));
    }

    @Override public @NotNull Restr.Disj<E> or(@NotNull Cond<E> cond) {
      return new Disj<>(orz.appended(new Conj<>(ImmutableSeq.of(cond))));
    }

    @Override public <T extends TermLike<T>> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Disj<>(orz.map(x -> new Conj<>(x.ands.map(f))));
    }
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Restr<E> cof) {
    return switch (cof) {
      case Restr.Const<E> c -> Doc.symbol(c.isOne ? "0=0" : "0=1");
      case Restr.Disj<E> disj -> toDoc(disj);
    };
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Disj<E> cof) {
    return Doc.join(Doc.spaced(Doc.symbol("\\/")), cof.orz.view().map(or ->
      or.ands.sizeGreaterThan(1) && cof.orz.sizeGreaterThan(1)
        ? Doc.parened(toDoc(or)) : toDoc(or)));
  }
  record Const<E extends TermLike<E>>(boolean isOne) implements Restr<E> {
    @Override public @NotNull SeqView<E> instView() {
      return SeqView.empty();
    }

    @Override public @NotNull Const<E> map(@NotNull Function<E, E> g) {
      return this;
    }

    @Override public @NotNull <T extends TermLike<T>>
    Restr.Const<T> fmap(@NotNull Function<E, T> g) {
      return new Const<>(isOne);
    }

    @Override public @NotNull Restr<E> or(@NotNull Cond<E> cond) {
      return isOne ? this : fromCond(cond);
    }

    @Override public <T extends TermLike<T>> Const<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Const<>(isOne);
    }
  }
  static <E extends TermLike<E>> @NotNull Disj<E> fromCond(Cond<E> cond) {
    return new Disj<>(ImmutableSeq.of(new Conj<>(ImmutableSeq.of(cond))));
  }
  record Cond<E>(@NotNull E inst, boolean isOne) implements Serializable {
    public Cond<E> map(@NotNull Function<E, E> g) {
      var apply = g.apply(inst);
      if (apply == inst) return this;
      return new Cond<>(apply, isOne);
    }

    @Contract("_ -> new") public <To extends TermLike<To>>
    @NotNull Cond<To> fmap(@NotNull Function<E, To> g) {
      return new Cond<>(g.apply(inst), isOne);
    }
  }
  record Conj<E extends TermLike<E>>(@NotNull ImmutableSeq<Cond<E>> ands) implements Serializable {

    public Conj<E> map(@NotNull Function<E, E> g) {
      var newAnds = ands.map(c -> c.map(g));
      if (newAnds.sameElements(ands, true)) return this;
      return new Conj<>(newAnds);
    }

    @Contract("_ -> new") public <To extends Restr.TermLike<To>>
    @NotNull Conj<To> fmap(@NotNull Function<E, To> g) {
      return new Conj<>(ands.map(c -> c.fmap(g)));
    }

    public @NotNull SeqView<E> view() {
      return ands.view().map(and -> and.inst);
    }

    public Conj<E> and(@NotNull Restr.Conj<E> cof) {
      return new Conj<>(ands.appendedAll(cof.ands));
    }
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Conj<E> cof) {
    return Doc.join(Doc.spaced(Doc.symbol("/\\")), cof.ands.view().map(and -> Doc.sep(and.inst.toDoc(), Doc.symbol("="), Doc.symbol(!and.isOne() ? "0" : "1"))));
  }

  record Side<E extends TermLike<E>>(@NotNull Restr.Conj<E> cof, @NotNull E u) implements Serializable {

    public Side<E> rename(@NotNull Function<E, E> g) {
      var apply = g.apply(u);
      var newCof = cof.map(g);
      if (apply == u && newCof == cof) return this;
      return new Side<>(newCof, apply);
    }

    @Contract("_ -> new") public <To extends Restr.TermLike<To>>
    @NotNull Side<To> fmap(@NotNull Function<E, To> g) {
      return new Side<>(cof.fmap(g), g.apply(u));
    }
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Side<E> side) {
    return Doc.sep(toDoc(side.cof), Doc.symbol("|->"), side.u.toDoc());
  }

  /**
   * <a href="https://github.com/mortberg/cubicaltt/blob/a5c6f94bfc0da84e214641e0b87aa9649ea114ea/Connections.hs#L178-L197">cubicaltt</a>
   */
  static <T extends TermLike<T>> T formulae(Formula<T> formula, TermLike.Factory<T> factory) {
    return switch (formula) { // de Morgan laws
      // ~ 1 = 0, ~ 0 = 1
      case Formula.Inv<T> inv && inv.i().asFormula() instanceof Formula.Lit<T> lit ->
        factory.apply(new Formula.Lit<>(!lit.isOne()));
      // ~ (~ a) = a
      case Formula.Inv<T> inv && inv.i().asFormula() instanceof Formula.Inv<T> ii -> ii.i(); // DNE!! :fear:
      // ~ (a /\ b) = (~ a \/ ~ b), ~ (a \/ b) = (~ a /\ ~ b)
      case Formula.Inv<T> inv && inv.i().asFormula() instanceof Formula.Conn<T> conn ->
        factory.apply(new Formula.Conn<>(!conn.isAnd(),
          formulae(new Formula.Inv<>(conn.l()), factory),
          formulae(new Formula.Inv<>(conn.r()), factory)));
      // 0 /\ a = 0, 1 /\ a = a, 0 \/ a = a, 1 \/ a = 1
      case Formula.Conn<T> conn && conn.l().asFormula() instanceof Formula.Lit<T> l -> !l.isOne()
        ? (conn.isAnd() ? conn.l() : conn.r())
        : (conn.isAnd() ? conn.r() : conn.l());
      // a /\ 0 = 0, a /\ 1 = a, a \/ 0 = a, a \/ 1 = 1
      case Formula.Conn<T> conn && conn.r().asFormula() instanceof Formula.Lit<T> r -> !r.isOne()
        ? (conn.isAnd() ? conn.r() : conn.l())
        : (conn.isAnd() ? conn.l() : conn.r());
      default -> factory.apply(formula);
    };
  }
}
