package org.aya.guest0x0.util;

import org.aya.guest0x0.syntax.DefVar;
import org.jetbrains.annotations.NotNull;

public sealed interface AnyVar permits DefVar, LocalVar {
  @NotNull String name();
}
