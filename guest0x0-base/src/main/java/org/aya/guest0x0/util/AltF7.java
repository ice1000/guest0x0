package org.aya.guest0x0.util;

import org.aya.guest0x0.syntax.BdryData;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public record AltF7(@NotNull LocalVar var) {
  public boolean press(@NotNull Term term) {
    return switch (term) {
      case Term.Ref r -> r.var() == var;
      case Term.Lam lam -> press(lam.body());
      case Term.Transp transp -> press(transp.cover());
      case Term.PCall pCall -> press(pCall.p()) || pCall.i().anyMatch(this::press) || boundaries(pCall.b());
      case Term.PLam pLam -> press(pLam.fill());
      case Term.Call call -> call.fn() == var || call.args().anyMatch(this::press);
      case Term.Two two -> press(two.f()) || press(two.a());
      case Term.Proj proj -> press(proj.t());
      case Term.UI ignored -> false;
      case Term.DT dt -> press(dt.param().type()) || press(dt.cod());
      case Term.Path path -> boundaries(path.data());
      case Term.Mula mula -> formula(mula.asFormula());
    };
  }

  private boolean boundaries(BdryData<Term> data) {
    return press(data.type()) || data.boundaries().anyMatch(b -> press(b.body()));
  }

  private boolean formula(Formula<Term> formula) {
    return switch (formula) {
      case Formula.Lit ignored -> false;
      case Formula.Inv<Term> inv -> press(inv.i());
      case Formula.Conn<Term> conn -> press(conn.l()) || press(conn.r());
    };
  }
}
