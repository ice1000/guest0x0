package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public record Elaborator(
  @NotNull MutableMap<LocalVar, Term> env
) {
  record Synth(@NotNull Term wellTyped, @NotNull Term type) {
  }

  public @NotNull Synth synth(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.Trebor trebor -> new Synth(new Term.U(), new Term.U());
      case Expr.Resolved resolved -> new Synth(new Term.Ref(resolved.ref()), env.get(resolved.ref()));
      case Expr.Proj proj -> {
        var synth = synth(proj);
        if (!(synth.type instanceof Term.DT dt) || dt.isPi())
          throw new IllegalArgumentException("Expects a sigma type, got: " + synth.type);
        var fst = new Term.Proj(synth.wellTyped, 1);
        if (proj.oneOrTwo() == 1) yield new Synth(fst, dt.param().type());
        yield new Synth(new Term.Proj(synth.wellTyped, 2), dt.cod().subst(dt.param().x(), fst));
      }
      default -> throw new IllegalArgumentException("Does not support inferring: " + expr);
    };
  }
}
