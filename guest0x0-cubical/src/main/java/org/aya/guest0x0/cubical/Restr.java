package org.aya.guest0x0.cubical;

import kala.collection.immutable.ImmutableSeq;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Face restrictions.
 *
 * @param <E> "terms"
 * @see CofThy for cofibration operations
 */
public sealed interface Restr<E extends Restr.TermLike<E>> extends Docile {
  interface TermLike<E extends TermLike<E>> extends Docile {
    default @Nullable Formula<E> asFormula() {return null;}
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



    @Override public @NotNull Doc toDoc() {
      return Doc.join(spaced(Doc.symbol("\\/")), orz.view().map(or -> {
        var orDoc = Doc.join(spaced(Doc.symbol("/\\")), or.ands.view().map(and ->
          Doc.sep(and.inst.toDoc(), Doc.symbol("="), Doc.symbol(and.isLeft() ? "0" : "1"))));
        return or.ands.sizeGreaterThan(1) && orz.sizeGreaterThan(1)
          ? Doc.parened(orDoc) : orDoc;
      }));
    }

    private @NotNull Doc spaced(Doc symbol) {
      return Doc.cat(Doc.ONE_WS, symbol, Doc.ONE_WS);
    }
  }
  record Const<E extends TermLike<E>>(boolean isTrue) implements Restr<E> {
    @Override public Const<E> fmap(@NotNull Function<E, E> g) {
      return this;
    }

    @Override public @NotNull Doc toDoc() {
      return Doc.symbol(isTrue ? "0=0" : "0=1");
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
