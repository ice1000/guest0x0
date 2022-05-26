package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.cubical.Boundary;
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
        yield term(lam.body());
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
          yield pLam.dims().sizeEquals(1) ? fill : new Term.PLam(pLam.dims().drop(1), fill);
        }
        var heaven = stairway(pApp.b(), i); // Important: use unnormalized pApp.b()
        yield heaven != null ? heaven : new Term.PCall(p, i, pApp.b().fmap(this::term));
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
      case Term.PartEl par -> {
        var clauses = MutableArrayList.<Restr.Side<Term>>create();
        for (var clause : par.clauses()) {
          var u = term(clause.u());
          CofThy.normalizeCof(clause.cof(), clauses, cofib -> new Restr.Side<>(cofib, u));
        }
        yield new Term.PartEl(clauses.toImmutableArray());
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

  // https://github.com/mortberg/cubicaltt/blob/a5c6f94bfc0da84e214641e0b87aa9649ea114ea/Connections.hs#L178-L197
  private Term formulae(Formula<Term> formula) {
    return switch (formula) { // de Morgan laws
      // ~ 1 = 0, ~ 0 = 1
      case Formula.Inv<Term> inv && inv.i().asFormula() instanceof Formula.Lit<Term> lit -> Term.end(!lit.isLeft());
      // ~ (~ a) = a
      case Formula.Inv<Term> inv && inv.i().asFormula() instanceof Formula.Inv<Term> ii -> ii.i(); // DNE!! :fear:
      // ~ (a /\ b) = (~ a \/ ~ b), ~ (a \/ b) = (~ a /\ ~ b)
      case Formula.Inv<Term> inv && inv.i().asFormula() instanceof Formula.Conn<Term> conn ->
        new Term.Mula(new Formula.Conn<>(!conn.isAnd(),
          formulae(new Formula.Inv<>(conn.l())),
          formulae(new Formula.Inv<>(conn.r()))));
      // 0 /\ a = 0, 1 /\ a = a, 0 \/ a = a, 1 \/ a = 1
      case Formula.Conn<Term> conn && conn.l() instanceof Term.Mula lf
        && lf.asFormula() instanceof Formula.Lit<Term> l -> l.isLeft()
        ? (conn.isAnd() ? lf : conn.r())
        : (conn.isAnd() ? conn.r() : lf);
      // a /\ 0 = 0, a /\ 1 = a, a \/ 0 = a, a \/ 1 = 1
      case Formula.Conn<Term> conn && conn.r() instanceof Term.Mula rf
        && rf.asFormula() instanceof Formula.Lit<Term> r -> r.isLeft()
        ? (conn.isAnd() ? rf : conn.l())
        : (conn.isAnd() ? conn.l() : rf);
      default -> new Term.Mula(formula);
    };
  }

  /** She's buying a stairway to heaven. */
  private @Nullable Term stairway(
    @NotNull BdryData<Term> thoughts,
    @NotNull ImmutableSeq<Term> word // With a word she can get what she came for.
  ) {
    assert word.sizeEquals(thoughts.dims().size());
    for (var thought : thoughts.boundaries()) {
      var reason = piper(word, thought.face(), thoughts.dims());
      if (reason != null) return reason.term(thought.body());
    }
    return null; // Sometimes all of our thoughts are misgiven.
  }

  /** A piper that will lead us to reason. */
  private Normalizer piper(ImmutableSeq<Term> word, Boundary.Face face, ImmutableSeq<LocalVar> dims) {
    var sign = derive();
    for (var ct : face.pats().zipView(dims.zipView(word))) {
      if (ct._1 == Boundary.Case.VAR) sign.rho.put(ct._2);
      else if (!(ct._2._2.asFormula() instanceof Formula.Lit<Term> lit
        && lit.isLeft() == (ct._1 == Boundary.Case.LEFT))) return null;
    }
    return sign;
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
        case Term.Ref ref -> new Term.Ref(map.getOrDefault(ref.var(), ref.var()));
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
        case Term.PCall pApp -> new Term.PCall(term(pApp.p()), pApp.i().map(this::term), boundaries(pApp.b()));
        case Term.Mula f -> new Term.Mula(f.asFormula().fmap(this::term));
        case Term.Transp tr -> new Term.Transp(term(tr.cover()), tr.restr().fmap(this::term));
        case Term.Cof cof -> cof.fmap(this::term);
        case Term.PartTy par -> new Term.PartTy(term(par.ty()), term(par.restr()));
        case Term.PartEl par -> new Term.PartEl(par.clauses().map(clause -> clause.rename(this::term)));
      };
    }

    private @NotNull BdryData<Term> boundaries(@NotNull BdryData<Term> data) {
      return data.fmap(this::term, data.dims().map(this::param));
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
