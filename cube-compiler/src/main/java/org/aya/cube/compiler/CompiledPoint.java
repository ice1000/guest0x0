package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public record CompiledPoint(byte @NotNull [] latex) implements Serializable {
}
