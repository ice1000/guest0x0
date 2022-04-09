package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Param;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public interface Unifier {
  static boolean untyped(@NotNull Term l, @NotNull Term r) {
    return switch (l) {
      case Term.Lam lam && r instanceof Term.Lam ram ->
        untyped(lam.body(), rhs(ram.body(), ram.param().x(), lam.param()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll && r instanceof Term.Lam ram -> eta(ll, ram);
      case Term.Ref lref && r instanceof Term.Ref rref -> lref.var() == rref.var();
      case Term.Two lapp && r instanceof Term.Two rapp ->
        lapp.isApp() == rapp.isApp() && untyped(lapp.f(), rapp.f()) && untyped(lapp.a(), rapp.a());
      case Term.DT ldt && r instanceof Term.DT rdt -> ldt.isPi() == rdt.isPi()
        && untyped(ldt.param().type(), rdt.param().type())
        && untyped(ldt.cod(), rhs(rdt.cod(), rdt.param().x(), ldt.param()));
      case Term.Proj lproj && r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untyped(lproj.t(), rproj.t());
      case Term.UI lu && r instanceof Term.UI ru -> lu.isU() == ru.isU();
      case Term.Call lcall && r instanceof Term.Call rcall -> lcall.fn() == rcall.fn()
        && lcall.args().sameElements(rcall.args(), true);
      case Term.End lend && r instanceof Term.End rend -> lend.isLeft() == rend.isLeft();
      case Term.PLam plam && r instanceof Term.PLam pram && plam.dims().sizeEquals(pram.dims()) ->
        untyped(plam.fill(), pram.fill().subst(MutableMap.from(
          pram.dims().zip(plam.dims()).map(p -> Tuple.of(p._1, new Term.Ref(p._2))))));
      // Cubical subtyping?? Are we ever gonna unify cubes?
      default -> false;
    };
  }
  private static boolean eta(@NotNull Term r, Term.Lam lam) {
    return untyped(lam.body(), Term.mkApp(r, new Term.Ref(lam.param().x())));
  }

  private static @NotNull Term rhs(Term body, LocalVar x, Param<Term> param) {
    return body.subst(x, new Term.Ref(param.x()));
  }
}
