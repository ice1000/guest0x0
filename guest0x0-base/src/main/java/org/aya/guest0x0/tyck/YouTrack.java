package org.aya.guest0x0.tyck;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.aya.util.error.SourcePos;
import org.aya.util.tyck.MCT;
import org.jetbrains.annotations.NotNull;

/** YouTrack checks Confluence. This file uses OOP in the evilest way. */
public record YouTrack(
  @NotNull SeqView<LocalVar> dims,
  @NotNull ImmutableSeq<MCT.SubPats<Boundary.Case>> subPatsSeq
) {
  public YouTrack(@NotNull Boundary.Data<Term> d) {
    this(d.dims().view(), d.boundaries().mapIndexed((i, b) -> new MCT.SubPats<>(b.pats().view(), i)));
  }

  private MCT<LocalVar, Void> celebrate() { // One more time
    return MCT.classify(dims, subPatsSeq, (lv, sp) -> new YouTrack(lv, sp).cleave());
  }

  private MCT<LocalVar, Void> cleave() { // Never errors -- the raw syntax is sound
    var buffer = MutableArrayList.<MCT<LocalVar, Void>>create(2);
    if (subPatsSeq.allMatch(p -> p.head() == Boundary.Case.VAR)) return null; // Lay flat
    for (var end : Seq.of(Boundary.Case.LEFT, Boundary.Case.RIGHT)) {
      var indices = new MCT.Leaf<>(subPatsSeq.mapIndexedNotNull((i, p) -> p.head() != end ? i : null));
      var classes = MCT.extract(indices, subPatsSeq).map(MCT.SubPats::drop);
      if (classes.isNotEmpty()) buffer.append(new YouTrack(dims.drop(1), classes).celebrate());
    }
    return new MCT.Node<>(dims.first(), buffer.toImmutableArray());
  }
}
