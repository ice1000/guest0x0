package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;

import static org.aya.guest0x0.syntax.Boundary.Case.*;
import static org.aya.guest0x0.syntax.Term.*;

/**
 * References:
 * <ul>
 * <li><a href="https://github.com/molikto/mlang/blob/5110e18d20484a3f4ee57ee68e2793e5cf0e28e6/src-main/src/main/scala/mlang/compiler/semantic/10_value_fibrant.scala">10_value_fibrant.scala</a></li>
 * <li><a href="https://www.cse.chalmers.se/~simonhu/misc/hcomp.pdf">hcomp.pdf</a></li>
 * </ul>
 */
public interface HCompPDF {
  record Transps(@NotNull Term cover, @NotNull ImmutableSeq<Term> args, @NotNull Boundary.Cof cof) {
    public @NotNull Term inv() {
      return new Transp(mkLam("i", i -> mkApp(cover, neg(i))), cof, args);
    }

    public @NotNull Term fill(@NotNull LocalVar i) {
      return new Transp(mkLam("j", j -> mkApp(cover, and(new Ref(i), j))),
        amendCof(i, LEFT), args.appended(new Ref(i)));
    }

    public @NotNull Term invFill(@NotNull LocalVar i) {
      return new Transp(mkLam("j", j -> mkApp(cover, neg(and(neg(new Ref(i)), j)))),
        amendCof(i, RIGHT), args.appended(new Ref(i)));
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
