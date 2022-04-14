package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;

public interface HCompPDF {
  record Transps(@NotNull Term cover, @NotNull ImmutableSeq<Term> args, @NotNull Boundary.Cof cof) {
    public @NotNull Term inv() {
      var x = new LocalVar("i");
      return new Term.Transp(new Term.Lam(x, Term.mkApp(cover, Term.inv(new Term.Ref(x)))), cof, args);
    }
  }
}
