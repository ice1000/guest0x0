package org.aya.guest0x0.syntax;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.util.LocalVar;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Boundary<E>(@NotNull Face face, @NotNull E body) {
  public enum Case {
    LEFT, RIGHT, VAR
  }

  public sealed interface Cond<E> {
    Cond<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g);
    record Eq<E>(@NotNull LocalVar i, @NotNull E inst, boolean isLeft) implements Cond<E> {
      @Override public Eq<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
        return new Eq<>(f.apply(i), g.apply(inst), isLeft);
      }
    }
    record Const<E>(boolean isTrue) implements Cond<E> {
      @Override public Const<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
        return this;
      }
    }
  }
  public record Cofib<E>(@NotNull ImmutableSeq<Cond<E>> ands) {
    public Cofib<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
      return new Cofib<>(ands.map(c -> c.rename(f, g)));
    }
  }
  public record Psi<E>(@NotNull ImmutableSeq<Cofib<E>> orz) {
    public @NotNull Psi<E> rename(@NotNull Function<LocalVar, LocalVar> f, @NotNull Function<E, E> g) {
      return new Psi<>(orz.map(x -> x.rename(f, g)));
    }
  }

  public record Face(@NotNull ImmutableSeq<Case> pats) implements Docile {
    @Override public @NotNull Doc toDoc() {
      var zesen = MutableList.of(Doc.symbol("|"));
      pats.forEach(d -> zesen.append(Doc.symbol(switch (d) {
        case LEFT -> "0";
        case RIGHT -> "1";
        case VAR -> "_";
      })));
      return Doc.sep(zesen);
    }
  }

  public <T> @NotNull Boundary<T> fmap(@NotNull Function<E, T> f) {
    return new Boundary<>(face, f.apply(body));
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
      dims.forEach(d -> head.append(Doc.plain(d.name())));
      head.appendAll(new Doc[]{Doc.symbol("|]"), type.toDoc()});
      return Doc.cblock(Doc.sep(head), 2, Doc.vcat(boundaries.map(b ->
        Doc.sep(b.face.toDoc(), Doc.symbol("=>"), b.body().toDoc()))));
    }
  }
}

