package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Boundary<E>(@NotNull ImmutableSeq<Case> pats, @NotNull E body) {
  public enum Case {
    LEFT, RIGHT, VAR
  }

  public <T> @NotNull Boundary<T> fmap(@NotNull Function<E, T> f) {
    return new Boundary<>(pats, f.apply(body));
  }

  public record Data<E extends Docile>(
    @NotNull ImmutableSeq<LocalVar> dims,
    @NotNull E type,
    @NotNull ImmutableSeq<Boundary<E>> boundaries
  ) implements Docile {
    public @NotNull Data<E> fmap(@NotNull Function<E, E> f, @NotNull ImmutableSeq<LocalVar> newDims) {
      return new Data<>(newDims, f.apply(type), boundaries.map(b -> b.fmap(f)));
    }

    public @NotNull Data<E> fmap(@NotNull Function<E, E> f) {
      return fmap(f, dims);
    }

    @Override public @NotNull Doc toDoc() {
      var head = MutableList.of(Doc.symbol("[|"));
      dims.forEach(d -> head.append(Doc.symbol(d.name())));
      head.appendAll(new Doc[]{Doc.symbol("|]"), type.toDoc()});
      return Doc.cblock(Doc.sep(head), 2, Doc.vcat(boundaries.map(b -> {
        var zesen = MutableList.of(Doc.symbol("|"));
        b.pats().forEach(d -> zesen.append(Doc.symbol(switch (d) {
          case LEFT -> "0";
          case RIGHT -> "1";
          case VAR -> "_";
        })));
        zesen.append(Doc.symbol("=>"));
        zesen.append(b.body().toDoc());
        return Doc.sep(zesen);
      })));
    }
  }

  public sealed interface Formula<E extends Docile> {
    @NotNull Formula<E> fmap(@NotNull Function<E, E> f);
  }
  /** @param isAnd it's or if false */
  public record Conn<E extends Docile>(boolean isAnd, @NotNull E l, @NotNull E r) implements Formula<E> {
    public @NotNull Conn<E> fmap(@NotNull Function<E, E> f) {
      return new Conn<>(isAnd, f.apply(l), f.apply(r));
    }
  }
  public record Inv<E extends Docile>(@NotNull E i) implements Formula<E> {
    public @NotNull Inv<E> fmap(@NotNull Function<E, E> f) {
      return new Inv<>(f.apply(i));
    }
  }
}

