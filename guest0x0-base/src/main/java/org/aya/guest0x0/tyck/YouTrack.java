package org.aya.guest0x0.tyck;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.primitive.IntObjTuple2;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.SPE;
import org.aya.pretty.doc.Doc;
import org.aya.util.error.SourcePos;
import org.aya.util.tyck.MCT;
import org.jetbrains.annotations.NotNull;

/** YouTrack checks Confluence. This file uses OOP in the evilest way. */
public interface YouTrack {
  static void jesperCockx(@NotNull Boundary.Data<Term> d, @NotNull SourcePos pos) {
    celebrate(d.dims().view(), d.boundaries().mapIndexed((i, b) -> new MCT.SubPats<>(b.pats().view(), i)))
      .forEach(cls -> {
        var contents = cls.contents().map(i -> IntObjTuple2.of(i, d.boundaries().get(i)));
        for (int i = 1; i < contents.size(); i++) {
          var a = contents.get(i);
          for (int j = 0; j < i; j++) {
            var b = contents.get(j);
            var unifier = new Unifier.Cof(MutableMap.create());
            unifier.unify(d.dims(), a._2.pats(), b._2.pats());
            if (!Unifier.untyped(unifier.l().term(a._2.body()), unifier.r().term(b._2.body())))
              throw new SPE(pos, Doc.plain("The"), Doc.ordinal(a._1 + 1), Doc.plain("and"), Doc.ordinal(b._1 + 1),
                Doc.plain("boundaries do not agree!!"));
          }
        }
      });
  }

  /** One more time */
  private static MCT<LocalVar, Void> celebrate(SeqView<LocalVar> dims, ImmutableSeq<MCT.SubPats<Boundary.Case>> sub) {
    return MCT.classify(dims, sub, YouTrack::cleave);
  }

  /** Never errors -- the raw syntax is sound */
  private static MCT<LocalVar, Void> cleave(SeqView<LocalVar> dims, ImmutableSeq<MCT.SubPats<Boundary.Case>> sub) {
    var buffer = MutableArrayList.<MCT<LocalVar, Void>>create(2);
    if (sub.allMatch(p -> p.head() == Boundary.Case.VAR)) return null; // Lay flat
    for (var end : Seq.of(Boundary.Case.LEFT, Boundary.Case.RIGHT)) {
      var indices = new MCT.Leaf<>(sub.mapIndexedNotNull((i, p) -> p.head() != end ? i : null));
      var classes = MCT.extract(indices, sub).map(MCT.SubPats::drop);
      if (classes.isNotEmpty()) buffer.append(celebrate(dims.drop(1), classes));
    }
    return new MCT.Node<>(dims.first(), buffer.toImmutableArray());
  }
}
