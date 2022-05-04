package org.aya.guest0x0.cubical;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record Boundary<E>(@NotNull Face face, @NotNull E body) {
  public enum Case {
    LEFT, RIGHT, VAR
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
}

