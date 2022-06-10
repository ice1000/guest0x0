package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public interface TextBuilder {
  void append(@NotNull String text, boolean highlight);
  default void append(byte @NotNull [] text, boolean highlight) {
    append(new String(text, StandardCharsets.US_ASCII), highlight);
  }
  default void appendln(@NotNull String text, boolean highlight) {
    append(text, highlight);
    append("\n", false);
  }

  record Strings(@NotNull StringBuilder sb) implements TextBuilder {
    public Strings() {this(new StringBuilder());}

    @Override public void append(@NotNull String text, boolean highlight) {
      sb.append(text);
    }
  }
}
