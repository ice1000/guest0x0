package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.cubical.CofThy;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.cubical.Partial;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.jetbrains.annotations.NotNull;

public class Unifier {
  public record FailureData(@NotNull Term l, @NotNull Term r) {}
  public FailureData data;

  boolean untyped(@NotNull Term l, @NotNull Term r) {
    if (l == r) return true;
    var happy = switch (l) {
      case Term.Lam lam && r instanceof Term.Lam ram -> untyped(lam.body(), rhs(ram.body(), ram.x(), lam.x()));
      case Term.Lam lam -> eta(r, lam);
      case Term ll && r instanceof Term.Lam ram -> eta(ll, ram);
      case Term.Ref lref && r instanceof Term.Ref rref -> lref.var() == rref.var();
      case Term.Two lapp && r instanceof Term.Two rapp ->
        lapp.isApp() == rapp.isApp() && untyped(lapp.f(), rapp.f()) && untyped(lapp.a(), rapp.a());
      case Term.DT ldt && r instanceof Term.DT rdt -> ldt.isPi() == rdt.isPi()
        && untyped(ldt.param().type(), rdt.param().type())
        && untyped(ldt.cod(), rhs(rdt.cod(), rdt.param().x(), ldt.param().x()));
      case Term.Proj lproj && r instanceof Term.Proj rproj ->
        lproj.isOne() == rproj.isOne() && untyped(lproj.t(), rproj.t());
      case Term.UI lu && r instanceof Term.UI ru -> lu.keyword() == ru.keyword();
      case Term.Call lcall && r instanceof Term.Call rcall -> lcall.fn() == rcall.fn()
        && lcall.args().sameElements(rcall.args(), true);
      case Term.PLam plam && r instanceof Term.PLam pram && plam.dims().sizeEquals(pram.dims()) ->
        untyped(plam.fill(), pram.fill().subst(MutableMap.from(
          pram.dims().zip(plam.dims()).map(p -> Tuple.of(p._1, new Term.Ref(p._2))))));
      case Term.PCall lpcall && r instanceof Term.PCall rpcall ->
        untyped(lpcall.p(), rpcall.p()) && unifySeq(lpcall.i(), rpcall.i());
      case Term.Mula lf && r instanceof Term.Mula rf -> formulae(lf.asFormula(), rf.asFormula());
      case Term.Transp ltp && r instanceof Term.Transp rtp ->
        untyped(ltp.cover(), rtp.cover()) && untyped(ltp.restr(), rtp.restr());
      case Term.Cof lcof && r instanceof Term.Cof rcof -> Normalizer.create().propExt(lcof.restr(), rcof.restr());
      case Term.PartTy lpart && r instanceof Term.PartTy rpart ->
        untyped(lpart.ty(), rpart.ty()) && untyped(lpart.restr(), rpart.restr());
      case Term.PartEl ll && r instanceof Term.PartEl rr -> untyped(ll.inner(), rr.inner());
      case Term.Sub ll && r instanceof Term.Sub rr -> untyped(ll.ty(), rr.ty())
        && untyped(ll.par(), rr.par());
      case Term.InS ll && r instanceof Term.InS rr -> untyped(ll.e(), rr.e());
      case Term.OutS ll && r instanceof Term.OutS rr -> untyped(ll.e(), rr.e());
      case Term.Hcomp ll && r instanceof Term.Hcomp rr ->
        untyped(ll.data().walls(), rr.data().walls()) && untyped(ll.data().bottom(), rr.data().bottom());
      // Cubical subtyping?? Are we ever gonna unify cubes?
      default -> false;
    };
    if (!happy && data == null)
      data = new FailureData(l, r);
    return happy;
  }

  private boolean untyped(Partial<Term> l, Partial<Term> r) {
    return switch (l) {
      case Partial.Const<Term> ll -> switch (r) {
        case Partial.Const<Term> rr -> untyped(ll.u(), rr.u());
        case Partial.Split<Term> rr -> untyped(r, l);
      };
      case Partial.Split<Term> ll -> ll.clauses().allMatch(clause -> clause(clause, new Term.PartEl(r)));
    };
  }

  /** Daniel Gratzer used <code>N</code> when explaining these to me */
  private boolean clause(@NotNull Restr.Side<Term> clause, @NotNull Term n) {
    return CofThy.conv(new Restr.Disj<>(clause.cof()),
      Normalizer.create(), subst -> untyped(new Term.PartEl(new Partial.Const<>(clause.u())), subst.term(n)));
  }

  private boolean unifySeq(@NotNull ImmutableSeq<Term> l, @NotNull ImmutableSeq<Term> r) {
    return l.zipView(r).allMatch(p -> untyped(p._1, p._2));
  }

  // Hopefully.... I don't know. :shrug:
  private boolean formulae(Formula<Term> lf, Formula<Term> rf) {
    return switch (lf) {
      case Formula.Lit<Term> l && rf instanceof Formula.Lit<Term> r -> l.isLeft() == !r.isOne();
      case Formula.Inv<Term> l && rf instanceof Formula.Inv<Term> r -> untyped(l.i(), r.i());
      case Formula.Conn<Term> l && rf instanceof Formula.Conn<Term> r && l.isAnd() == r.isAnd() ->
        untyped(l.l(), r.l()) && untyped(l.r(), r.r());
      default -> false;
    };
  }

  private boolean eta(@NotNull Term r, Term.Lam lam) {
    return untyped(lam.body(), r.app(new Term.Ref(lam.x())));
  }

  private static @NotNull Term rhs(Term rhs, LocalVar rb, LocalVar lb) {
    return rhs.subst(rb, new Term.Ref(lb));
  }
}
