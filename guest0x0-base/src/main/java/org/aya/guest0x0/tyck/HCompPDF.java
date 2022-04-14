package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
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
  static @NotNull Term powerBottom(@NotNull Boundary.Cof cof) {
    var cnf = MutableArrayList.<Term>create(cof.faces().size());
    for (var face : cof.faces()) {
      cnf.append(face.pats().zipView(cof.vars())
        .filter(t -> t._1 != VAR)
        .map(t -> t._1 == RIGHT ? new Ref(t._2) : neg(new Ref(t._2)))
        .reduce(Term::and));
    }
    // Default to "never constant" -- the left endpoint
    return cnf.isEmpty() ? end(true) : cnf.reduce(Term::or);
  }
  record Transps(
    @NotNull Term cover, @NotNull TranspData data
  ) {
    public @NotNull Term inv() {
      return new Transp(mkLam("i", i -> cover.app(neg(i))), data);
    }

    /** Marisa Kirisame!! */
    public @NotNull Term mk() {return new Transp(cover, data);}

    public @NotNull Term fill(@NotNull LocalVar i) {
      var ri = new Ref(i);
      return new Transp(mkLam("j", j -> cover.app(and(ri, j))),
        new TranspData(amendCof(i, LEFT), data.a().appended(ri), Term.or(data.psi(), neg(ri))));
    }

    public @NotNull Term invFill(@NotNull LocalVar i) {
      var ri = new Ref(i);
      return new Transp(mkLam("j", j -> cover.app(neg(and(neg(ri), j)))),
        new TranspData(amendCof(i, RIGHT), data.a().appended(ri), Term.or(data.psi(), ri)));
    }

    private @NotNull Boundary.Cof amendCof(@NotNull LocalVar i, @NotNull Boundary.Case at) {
      // Optimization potential: if i \in cof.vars, it is only necessary to add a new face.
      var newFaces = data.cof().faces().view()
        .map(face -> new Boundary.Face(face.pats().appended(VAR)))
        .appended(new Boundary.Face(ImmutableSeq.fill(data.cof().vars().size(), VAR).appended(at)))
        .toImmutableSeq();
      return new Boundary.Cof(data.cof().vars().appended(i), newFaces);
    }
  }
}
