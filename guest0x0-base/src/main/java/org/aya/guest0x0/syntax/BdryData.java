package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.cubical.Boundary;
import org.aya.guest0x0.util.LocalVar;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record BdryData<E extends Docile>(
  @NotNull ImmutableSeq<LocalVar> dims,
  @NotNull E type,
  @NotNull ImmutableSeq<Boundary<E>> boundaries
) implements Docile {
  public @NotNull BdryData<E> fmap(@NotNull Function<E, E> f, @NotNull ImmutableSeq<LocalVar> newDims) {
    return new BdryData<>(newDims, f.apply(type), boundaries.map(b -> b.fmap(f)));
  }

  public @NotNull BdryData<E> fmap(@NotNull Function<E, E> f) {
    return fmap(f, dims);
  }

  @Override public @NotNull Doc toDoc() {
    var head = MutableList.of(Doc.symbol("[|"));
    dims.forEach(d -> head.append(Doc.plain(d.name())));
    head.appendAll(new Doc[]{Doc.symbol("|]"), type.toDoc()});
    return Doc.cblock(Doc.sep(head), 2, Doc.vcat(boundaries.map(b ->
      Doc.sep(b.face().toDoc(), Doc.symbol("=>"), b.body().toDoc()))));
  }
}
