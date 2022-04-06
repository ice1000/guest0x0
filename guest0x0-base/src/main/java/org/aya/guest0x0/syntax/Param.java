package org.aya.guest0x0.syntax;

import org.jetbrains.annotations.NotNull;

public record Param<Term>(@NotNull LocalVar x, @NotNull Term type) {
}
