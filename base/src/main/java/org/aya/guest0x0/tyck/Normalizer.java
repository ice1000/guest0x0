package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.cubical.CofThy;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.BdryData;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Keyword;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.tyck.HCompPDF.Transps;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Normalizer(
  @NotNull MutableMap<LocalVar, Def<Term>> sigma,
  @NotNull MutableMap<LocalVar, Term> rho
) implements CofThy.SubstObj<Term, LocalVar, Normalizer> {
  public static @NotNull Normalizer create() {
    return new Normalizer(MutableMap.create(), MutableMap.create());
  }

  public static @NotNull Term rename(@NotNull Term term) {
    return new Renamer(MutableMap.create()).term(term);
  }

  public boolean propExt(Restr<Term> ll, Restr<Term> rr) {
    return CofThy.propExt(this, ll, rr, Normalizer::restr);
  }

  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  @Override public void put(LocalVar i, boolean isLeft) {
    rho.put(i, Term.end(isLeft));
  }

  @Override public boolean contradicts(LocalVar i, boolean newIsLeft) {
    if (!rho.containsKey(i)) return false;
    if (!(rho.get(i).asFormula() instanceof Formula.Lit<Term> lit)) return false;
    return lit.isLeft() != newIsLeft;
  }

  @Override public @Nullable LocalVar asRef(@NotNull Term term) {
    return term instanceof Term.Ref ref ? ref.var() : null;
  }

  @Override public @NotNull Normalizer derive() {
    return new Normalizer(sigma, MutableMap.from(rho));
  }

  public Term term(Term term) {
    return switch (term) {
      case Term.Ref ref -> rho.getOption(ref.var())
        .map(Normalizer::rename)
        .map(this::term).getOrDefault(ref);
      case Term.UI u -> u;
      case Term.Lam lam -> new Term.Lam(lam.x(), term(lam.body()));
      case Term.DT dt -> new Term.DT(dt.isPi(), param(dt.param()), term(dt.cod()));
      case Term.Two two -> {
        var f = term(two.f());
        var a = term(two.a());
        // Either a tuple or a stuck term is preserved
        if (!two.isApp() || !(f instanceof Term.Lam lam)) yield new Term.Two(two.isApp(), f, a);
        rho.put(lam.x(), a);
        var body = term(lam.body());
        rho.remove(lam.x());
        yield body;
      }
      case Term.Proj proj -> {
        var t = term(proj.t());
        if (!(t instanceof Term.Two tup)) yield new Term.Proj(t, proj.isOne());
        assert !tup.isApp();
        yield proj.isOne() ? tup.f() : tup.a();
      }
      case Term.Call call -> {
        var prefn = sigma.getOption(call.fn());
        if (!(prefn.getOrNull() instanceof Def.Fn<Term> fn)) yield call;
        fn.telescope().zip(call.args()).forEach(zip -> rho.put(zip._1.x(), term(zip._2)));
        yield term(fn.body());
      }
      case Term.Path path -> new Term.Path(path.data().fmap(this::term));
      case Term.PLam pLam -> new Term.PLam(pLam.dims(), term(pLam.fill()));
      case Term.PCall pApp -> {
        var i = pApp.i().map(this::term);
        var p = term(pApp.p());
        if (p instanceof Term.PLam pLam) {
          pLam.dims().zipView(i).forEach(rho::put);
          var fill = term(pLam.fill());
          pLam.dims().forEach(rho::remove);
          yield fill;
        }
        var b = pApp.b();
        // b.dims().zipView(i).forEach(zip -> rho.put(zip._1, zip._2));
        var partial = partial(b);
        if (partial instanceof Term.SomewhatPartial what) yield what.obvious();
        // b.dims().forEach(rho::remove);
        yield new Term.PCall(p, i, partial);
      }
      case Term.Mula f -> formulae(f.asFormula().fmap(this::term));
      case Term.Cof cof -> new Term.Cof(restr(cof.restr()));
      case Term.Transp transp -> {
        var parkerLiu = restr(transp.restr().restr());
        // Because of his talk about lax 2-functors!
        if (CofThy.satisfied(parkerLiu)) yield Term.id("x");
        yield transp(new LocalVar("i"), term(transp.cover()), new Term.Cof(parkerLiu));
      }
      case Term.PartTy par -> new Term.PartTy(term(par.ty()), term(par.restr()));
      case Term.PartEl par -> partial(par);
      case Term.Sub sub -> new Term.Sub(term(sub.ty()), partial(sub.par()));
      case Term.InS inS -> {
        var restr = restr(inS.restr());
        var e = term(inS.e());
        if (e instanceof Term.OutS outS && propExt(outS.par().restr(), restr))
          yield outS.e();
        yield new Term.InS(e, restr);
      }
      case Term.OutS outS -> {
        var partial = partial(outS.par());
        if (partial instanceof Term.SomewhatPartial what) yield what.obvious();
        var e = term(outS.e());
        if (e instanceof Term.InS inS) yield inS.e();
        yield new Term.OutS(e, partial);
      }
      case Term.Hcomp hcomp -> {
        var data = hcomp.data().fmap(this::term);
        if (data.walls().app(Term.end(false)) instanceof Term.SomewhatPartial p) yield p.obvious();
        yield new Term.Hcomp(data);
      }
    };
  }

  public @NotNull Term.PartEl partial(@NotNull Term.PartEl partial) {
    return switch (partial) {
      case Term.SomewhatPartial par -> new Term.SomewhatPartial(term(par.obvious()));
      case Term.ReallyPartial par -> {
        var clauses = MutableArrayList.<Restr.Side<Term>>create();
        for (var clause : par.clauses()) {
          var u = term(clause.u());
          if (CofThy.normalizeCof(clause.cof().fmap(this::term), clauses, cofib -> new Restr.Side<>(cofib, u))) {
            yield new Term.SomewhatPartial(u);
          }
        }
        yield new Term.ReallyPartial(clauses.toImmutableArray());
      }
    };
  }

  public Restr<Term> restr(@NotNull Restr<Term> restr) {
    return switch (restr.fmap(this::term)) {
      case Restr.Vary<Term> vary -> CofThy.normalizeRestr(vary);
      case Restr.Const<Term> c -> c;
    };
  }

  private Term transp(LocalVar i, Term cover, Term.Cof cof) {
    return switch (cover.app(new Term.Ref(i))) {
      case Term.DT dt && dt.isPi() -> Term.mkLam("f", u0 -> Term.mkLam("x", v -> {
        var laptop = new Transps(rename(new Term.Lam(i, dt.param().type())), cof);
        var w = laptop.invFill(i).app(v);
        // w0 = w.subst(i, 0), according to Minghao Liu
        var w0 = laptop.inv().app(v);
        var cod = rename(new Term.Lam(i, dt.codomain(w)));
        return new Term.Transp(cod, cof).app(u0.app(w0));
      }));
      case Term.UI u && u.keyword() == Keyword.U -> Term.id("u");
      case Term.DT dt /*&& !dt.isPi()*/ -> Term.mkLam("t", u0 -> {
        var laptop = new Transps(rename(new Term.Lam(i, dt.param().type())), cof);
        // Simon Huber wrote u0.1 both with and without parentheses, extremely confusing!!
        var v = laptop.fill(i).app(u0.proj(true));
        return new Term.Two(false, laptop.mk().app(u0.proj(true)),
          new Term.Transp(rename(new Term.Lam(i, dt.codomain(v))), cof).app(u0.proj(false)));
      });
      default -> new Term.Transp(cover, cof);
    };
  }

  private Term formulae(Formula<Term> formula) {
    return Restr.formulae(formula, Term.Mula::new);
  }

  record Renamer(MutableMap<LocalVar, LocalVar> map) {
    /** @implNote Make sure to rename param before bodying */
    public Term term(Term term) {
      return switch (term) {
        case Term.Lam lam -> {
          var param = param(lam.x());
          yield new Term.Lam(param, term(lam.body()));
        }
        case Term.UI u -> u;
        case Term.Ref ref -> new Term.Ref(vv(ref.var()));
        case Term.DT dt -> {
          var param = param(dt.param());
          yield new Term.DT(dt.isPi(), param, term(dt.cod()));
        }
        case Term.Two two -> new Term.Two(two.isApp(), term(two.f()), term(two.a()));
        case Term.Proj proj -> new Term.Proj(term(proj.t()), proj.isOne());
        case Term.Call call -> new Term.Call(call.fn(), call.args().map(this::term));
        case Term.Path path -> new Term.Path(boundaries(path.data()));
        case Term.PLam pLam -> {
          var params = pLam.dims().map(this::param);
          yield new Term.PLam(params, term(pLam.fill()));
        }
        case Term.PCall pApp -> new Term.PCall(term(pApp.p()), pApp.i().map(this::term), partEl(pApp.b()));
        case Term.Mula f -> new Term.Mula(f.asFormula().fmap(this::term));
        case Term.Transp tr -> new Term.Transp(term(tr.cover()), tr.restr().fmap(this::term));
        case Term.Cof cof -> cof.fmap(this::term);
        case Term.PartTy par -> new Term.PartTy(term(par.ty()), term(par.restr()));
        case Term.PartEl par -> partEl(par);
        case Term.Sub sub -> new Term.Sub(term(sub.ty()), partEl(sub.par()));
        case Term.InS inS -> new Term.InS(term(inS.e()), inS.restr().fmap(this::term));
        case Term.OutS outS -> new Term.OutS(term(outS.e()), partEl(outS.par()));
        case Term.Hcomp hcomp -> new Term.Hcomp(hcomp.data().fmap(this::term));
      };
    }

    private @NotNull Term.PartEl partEl(Term.PartEl par) {
      return switch (par) {
        case Term.ReallyPartial partial ->
          new Term.ReallyPartial(partial.clauses().map(clause -> clause.rename(this::term)));
        case Term.SomewhatPartial partial -> new Term.SomewhatPartial(term(partial.obvious()));
      };
    }

    private @NotNull BdryData<Term> boundaries(@NotNull BdryData<Term> data) {
      return data.fmap(this::term, data.dims().map(this::param));
    }

    private @NotNull LocalVar vv(@NotNull LocalVar var) {
      return map.getOrDefault(var, var);
    }

    private Param<Term> param(Param<Term> param) {
      return new Param<>(param(param.x()), term(param.type()));
    }

    private LocalVar param(LocalVar param) {
      var var = new LocalVar(param.name());
      map.put(param, var);
      return var;
    }
  }
}
