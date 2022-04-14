package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.Formula;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import static org.aya.guest0x0.syntax.Boundary.Case.*;

public interface HCompPDF {
  record Transps(@NotNull Term cover, @NotNull ImmutableSeq<Term> args, @NotNull Boundary.Cof cof) {
    public @NotNull Term inv() {
      return new Term.Transp(Term.mkLam("i", i -> Term.mkApp(cover, Term.inv(i))), cof, args);
    }

    public @NotNull Term fill(@NotNull LocalVar i) {
      return new Term.Transp(Term.mkLam("j", j -> Term.mkApp(cover,
        new Term.Mula(new Formula.Conn<>(true, new Term.Ref(i), j)))), amendCof(i, LEFT), args);
    }

    public @NotNull Term invFill(@NotNull LocalVar i) {
      return new Term.Transp(Term.mkLam("j", j -> Term.mkApp(cover,
        Term.inv(new Term.Mula(new Formula.Conn<>(true, Term.inv(new Term.Ref(i)), j))))), amendCof(i, RIGHT), args);
    }

    private @NotNull Boundary.Cof amendCof(@NotNull LocalVar i, @NotNull Boundary.Case at) {
      // Optimization potential: if i \in cof.vars, it is only necessary to add a new face.
      var newFaces = cof.faces().view()
        .map(face -> new Boundary.Face(face.pats().appended(VAR)))
        .appended(new Boundary.Face(ImmutableSeq.fill(cof.vars().size(), VAR).appended(at)))
        .toImmutableSeq();
      return new Boundary.Cof(cof.vars().appended(i), newFaces);
    }
  }
}
