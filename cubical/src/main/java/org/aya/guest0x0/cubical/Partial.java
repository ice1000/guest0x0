package org.aya.guest0x0.cubical;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Copied and paraphrased from Aya's implementation.
 *
 * @author imkiva
 */
public sealed interface Partial<Term extends Restr.TermLike<Term>> {
  /** Faces filled by this partial element */
  @NotNull Restr<Term> restr();
  @NotNull Partial<Term> map(@NotNull Function<Term, Term> mapper);
  /** Includes cofibrations and face terms */
  @NotNull SeqView<Term> termsView();

  /** I am happy because I have (might be) missing faces. Same as <code>ReallyPartial</code> in guest0x0 */
  record Happy<Term extends Restr.TermLike<Term>>(
    @NotNull ImmutableSeq<Restr.Side<Term>> clauses
  ) implements Partial<Term> {
    @Override public @NotNull Restr<Term> restr() {
      return new Restr.Vary<>(clauses.map(Restr.Side::cof));
    }

    @Override public @NotNull Happy<Term> map(@NotNull Function<Term, Term> mapper) {
      var cl = clauses.map(c -> c.rename(mapper));
      if (cl.sameElements(clauses, true)) return this;
      return new Happy<>(cl);
    }

    @Override public @NotNull SeqView<Term> termsView() {
      return clauses.view().flatMap(cl -> cl.cof().view().appended(cl.u()));
    }
  }

  /** I am sad because I am not partial. Same as <code>SomewhatPartial</code> in guest0x0 */
  record Sad<Term extends Restr.TermLike<Term>>(@NotNull Term u) implements Partial<Term> {
    @Override public @NotNull Restr<Term> restr() {
      return new Restr.Const<>(true);
    }

    @Override public @NotNull Partial.Sad<Term> map(@NotNull Function<Term, Term> mapper) {
      var u = mapper.apply(this.u);
      if (u == this.u) return this;
      return new Sad<>(u);
    }

    @Override public @NotNull SeqView<Term> termsView() {
      return SeqView.of(u);
    }
  }
}
