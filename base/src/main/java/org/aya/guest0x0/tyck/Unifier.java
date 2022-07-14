package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.cubical.Boundary;
import org.aya.guest0x0.cubical.CofThy;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.Def;
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
      case Term.Cof lcof && r instanceof Term.Cof rcof -> {
        var initial = Normalizer.create();
        var ll = lcof.restr();
        var rr = rcof.restr();
        yield CofThy.conv(ll, initial, normalizer -> CofThy.satisfied(normalizer.restr(rr)))
          && CofThy.conv(rr, initial, normalizer -> CofThy.satisfied(normalizer.restr(ll)));
      }
      case Term.PartTy lpart && r instanceof Term.PartTy rpart ->
        untyped(lpart.ty(), rpart.ty()) && untyped(lpart.restr(), rpart.restr());
      case Term.PartEl par -> par.clauses().allMatch(clause -> clause(clause, r));
      case Term ll && r instanceof Term.PartEl par -> par.clauses().allMatch(clause -> clause(clause, ll));
      // Cubical subtyping?? Are we ever gonna unify cubes?
      default -> false;
    };
    if (!happy && data == null)
      data = new FailureData(l, r);
    return happy;
  }

  /** Daniel Gratzer used <code>N</code> when explaining these to me */
  private boolean clause(@NotNull Restr.Side<Term> clause, @NotNull Term n) {
    return CofThy.conv(new Restr.Vary<>(ImmutableSeq.of(clause.cof())),
      Normalizer.create(), subst -> untyped(clause.u(), subst.term(n)));
  }

  private boolean unifySeq(@NotNull ImmutableSeq<Term> l, @NotNull ImmutableSeq<Term> r) {
    return l.zipView(r).allMatch(p -> untyped(p._1, p._2));
  }

  // Hopefully.... I don't know. :shrug:
  private boolean formulae(Formula<Term> lf, Formula<Term> rf) {
    return switch (lf) {
      case Formula.Lit<Term> l && rf instanceof Formula.Lit<Term> r -> l.isLeft() == r.isLeft();
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

  record Cof(@NotNull Normalizer l, @NotNull Normalizer r) {
    public Cof(@NotNull MutableMap<LocalVar, Def<Term>> sigma) {
      this(new Normalizer(sigma, MutableMap.create()), new Normalizer(sigma, MutableMap.create()));
    }

    public void unify(
      @NotNull ImmutableSeq<LocalVar> dims,
      @NotNull Boundary.Face lc, // Lambda calculus!!
      @NotNull Boundary.Face rc  // Reference counting!!
    ) {
      assert lc.pats().sizeEquals(dims) && rc.pats().sizeEquals(dims);
      for (var ttt : dims.zipView(lc.pats().zipView(rc.pats()))) {
        if (ttt._2._1 == ttt._2._2) continue;
        if (ttt._2._1 == Boundary.Case.VAR) r.rho().put(ttt._1, Term.end(ttt._2._2 == Boundary.Case.LEFT));
        if (ttt._2._2 == Boundary.Case.VAR) l.rho().put(ttt._1, Term.end(ttt._2._1 == Boundary.Case.LEFT));
      }
    }
  }
}
