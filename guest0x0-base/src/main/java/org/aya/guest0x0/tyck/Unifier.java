package org.aya.guest0x0.tyck;

import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public interface Unifier {
  static boolean untyped(@NotNull Term l, @NotNull Term r) {
    return switch (l) {
      case Term.Lam lam && r instanceof Term.Lam ram ->
        untyped(lam.body(), rhs(ram.body(), lam.param(), ram.param().x()));
      case Term.Ref lref && r instanceof Term.Ref rref -> lref == rref;
      case Term.Two lapp && r instanceof Term.Two rapp ->
        lapp.isApp() == rapp.isApp() && untyped(lapp.f(), rapp.f()) && untyped(lapp.a(), rapp.a());
      case Term.DT ldt && r instanceof Term.DT rdt -> untyped(ldt.param().type(), rdt.param().type())
        && untyped(ldt.cod(), rhs(rdt.cod(), ldt.param(), rdt.param().x()));
      case Term.Proj lproj && r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untyped(lproj.t(), rproj.t());
      case Term.U lu && r instanceof Term.U ru -> true;
      default -> false;
    };
  }

  private static @NotNull Term rhs(@NotNull Term body, @NotNull Term.Param param, @NotNull LocalVar x) {
    return body.subst(x, new Term.Ref(param.x()));
  }
}
