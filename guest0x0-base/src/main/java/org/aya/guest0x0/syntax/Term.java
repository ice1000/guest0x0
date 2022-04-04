package org.aya.guest0x0.syntax;

import org.jetbrains.annotations.NotNull;

public sealed interface Term {
  record Ref(@NotNull LocalVar var) implements Term {
  }
}
