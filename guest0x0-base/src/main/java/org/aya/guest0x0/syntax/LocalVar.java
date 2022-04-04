package org.aya.guest0x0.syntax;

import org.jetbrains.annotations.NotNull;

public record LocalVar(@NotNull String name) {
  public @NotNull String toString() {
    return name;
  }

  @Override public boolean equals(Object obj) {
    return this == obj;
  }

  @Override public int hashCode() {
    return System.identityHashCode(this);
  }
}
