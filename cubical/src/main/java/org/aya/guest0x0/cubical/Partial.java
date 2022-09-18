package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Copied and paraphrased from Aya's implementation.
 *
 * @author imkiva, ice1000
 */
public sealed interface Partial<Term extends Restr.TermLike<Term>> extends Serializable {
  /** Faces filled by this partial element */
  @NotNull Restr<Term> restr();
  /** @implNote unlike {@link Partial#fmap(Function)}, this method returns this when nothing changes. */
  @NotNull Partial<Term> map(@NotNull Function<Term, Term> mapper);
  @Contract("_->new") @NotNull <To extends Restr.TermLike<To>> Partial<To> fmap(@NotNull Function<Term, To> mapper);
  @NotNull Partial<Term> flatMap(@NotNull Function<Term, Term> mapper);
  /** Includes cofibrations and face terms */
  @NotNull SeqView<Term> termsView();

  /** I am happy because I have (might be) missing faces. Same as <code>ReallyPartial</code> in guest0x0 */
  record Split<Term extends Restr.TermLike<Term>>(
    @NotNull ImmutableSeq<Restr.Side<Term>> clauses
  ) implements Partial<Term> {
    @Override public @NotNull Restr<Term> restr() {
      return new Restr.Vary<>(clauses.map(Restr.Side::cof));
    }

    @Override public @NotNull Partial.Split<Term> map(@NotNull Function<Term, Term> mapper) {
      var cl = clauses.map(c -> c.rename(mapper));
      if (cl.sameElements(clauses, true)) return this;
      return new Split<>(cl);
    }

    @Override
    public @NotNull <To extends Restr.TermLike<To>>
    Partial.Split<To> fmap(@NotNull Function<Term, To> mapper) {
      return new Split<>(clauses.map(c -> c.fmap(mapper)));
    }

    @Override public @NotNull Partial<Term> flatMap(@NotNull Function<Term, Term> mapper) {
      var cl = MutableArrayList.<Restr.Side<Term>>create();
      for (var clause : clauses) {
        var u = mapper.apply(clause.u());
        if (CofThy.normalizeCof(clause.cof().map(mapper), cl, cofib -> new Restr.Side<>(cofib, u))) {
          return new Const<>(u);
        }
      }
      return new Split<>(cl.toImmutableArray());
    }

    @Override public @NotNull SeqView<Term> termsView() {
      return clauses.view().flatMap(cl -> cl.cof().view().appended(cl.u()));
    }
  }

  /** I am sad because I am not partial. Same as <code>SomewhatPartial</code> in guest0x0 */
  record Const<Term extends Restr.TermLike<Term>>(@NotNull Term u) implements Partial<Term> {
    @Override public @NotNull Restr<Term> restr() {
      return new Restr.Const<>(true);
    }

    @Override public @NotNull Partial.Const<Term> map(@NotNull Function<Term, Term> mapper) {
      var v = mapper.apply(u);
      if (v == u) return this;
      return new Const<>(v);
    }

    @Override public @NotNull <To extends Restr.TermLike<To>>
    Partial.Const<To> fmap(@NotNull Function<Term, To> mapper) {
      return new Const<>(mapper.apply(u));
    }

    @Override public @NotNull Partial<Term> flatMap(@NotNull Function<Term, Term> mapper) {
      return map(mapper);
    }

    @Override public @NotNull SeqView<Term> termsView() {
      return SeqView.of(u);
    }
  }
}
