package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
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
public sealed interface Restr<E extends Restr.TermLike<E>> {
  @NotNull SeqView<E> instView();
  interface TermLike<E extends TermLike<E>> {
    default @Nullable Formula<E> asFormula() {return null;}
  }
  Restr<E> fmap(@NotNull Function<E, E> g);
  Restr<E> or(Cond<E> cond);
  <T extends TermLike<T>> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f);
  record Vary<E extends TermLike<E>>(@NotNull ImmutableSeq<Cofib<E>> orz) implements Restr<E> {
    @Override public @NotNull SeqView<E> instView() {
      return orz.view().flatMap(Cofib::view);
    }

    @Override public Vary<E> fmap(@NotNull Function<E, E> g) {
      return new Vary<>(orz.map(x -> x.fmap(g)));
    }

    @Override public Vary<E> or(Cond<E> cond) {
      return new Vary<>(orz.appended(new Cofib<>(ImmutableSeq.of(cond))));
    }

    @Override public <T extends TermLike<T>> Restr<T> mapCond(@NotNull Function<Cond<E>, Cond<T>> f) {
      return new Vary<>(orz.map(x -> new Cofib<>(x.ands.map(f))));
    }
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Restr<E> cof) {
    return switch (cof) {
      case Restr.Const<E> c -> Doc.symbol(c.isTrue ? "0=0" : "0=1");
      case Restr.Vary<E> vary -> toDoc(vary);
    };
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Vary<E> cof) {
    return Doc.join(Doc.spaced(Doc.symbol("\\/")), cof.orz.view().map(or ->
      or.ands.sizeGreaterThan(1) && cof.orz.sizeGreaterThan(1)
        ? Doc.parened(toDoc(or)) : toDoc(or)));
  }
  record Const<E extends TermLike<E>>(boolean isTrue) implements Restr<E> {
    @Override public @NotNull SeqView<E> instView() {
      return SeqView.empty();
    }

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
    public Cond<E> fmap(@NotNull Function<E, E> g) {
      return new Cond<>(g.apply(inst), isLeft);
    }
  }
  record Cofib<E extends TermLike<E>>(@NotNull ImmutableSeq<Cond<E>> ands) {

    public Cofib<E> fmap(@NotNull Function<E, E> g) {
      return new Cofib<>(ands.map(c -> c.fmap(g)));
    }

    public @NotNull SeqView<E> view() {
      return ands.view().map(and -> and.inst);
    }

    public Cofib<E> and(@NotNull Cofib<E> cof) {
      return new Cofib<>(ands.appendedAll(cof.ands));
    }
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Cofib<E> cof) {
    return Doc.join(Doc.spaced(Doc.symbol("/\\")), cof.ands.view().map(and ->
      Doc.sep(and.inst.toDoc(), Doc.symbol("="), Doc.symbol(and.isLeft() ? "0" : "1"))));
  }

  record Side<E extends TermLike<E>>(@NotNull Cofib<E> cof, @NotNull E u) {

    public Side<E> rename(@NotNull Function<E, E> g) {
      return new Side<>(cof.fmap(g), g.apply(u));
    }
  }
  static <E extends TermLike<E> & Docile> @NotNull Doc toDoc(Side<E> side) {
    return Doc.sep(toDoc(side.cof), Doc.symbol("|->"), side.u.toDoc());
  }
}
