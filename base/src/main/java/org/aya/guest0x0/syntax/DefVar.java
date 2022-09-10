package org.aya.guest0x0.syntax;

import org.aya.guest0x0.util.AnyVar;
import org.jetbrains.annotations.NotNull;

/**
 * Use with <strong>extreme</strong> caution: the field {@link #core}
 * can be assigned only once after the core is generated.
 * We shall not copy any well-typed {@link Def} to avoid
 * the same {@link DefVar} semantically corresponds to different {@link Def}s.
 * <p>
 * For concrete (pre-elaboration) defs, we can copy them, and that's only
 * because we do not store concrete defs in {@link DefVar}. In case we do,
 * we have to make ASTs <strong>mutable</strong> if we want to mutate them.
 */
public final class DefVar<D extends Def> implements AnyVar {
  public D core;
  public Def.Signature signature;
  public final @NotNull String name;

  public DefVar(@NotNull String name) {
    this.name = name;
  }

  @Override public @NotNull String name() {
    return name;
  }
}
