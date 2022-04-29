package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.*;
import org.aya.guest0x0.tyck.HCompPDF.Transps;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record Normalizer(
  @NotNull MutableMap<LocalVar, Def<Term>> sigma,
  @NotNull MutableMap<LocalVar, Term> rho
) {
  public static @NotNull Term rename(@NotNull Term term) {
    return new Renamer(MutableMap.create()).term(term);
  }

  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  public @NotNull Normalizer derive() {
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
      case Term.Mula f -> formulae(f.formula().fmap(this::term));
      case Term.Transp transp -> {
        var parkerLiu = restr(transp.restr());
        // Because of his talk about lax 2-functors!
        if (new CofThy(parkerLiu).satisfied()) yield Term.id("x");
        yield transp(new LocalVar("i"), term(transp.cover()), parkerLiu);
      }
    };
  }

  /**
   * Normalizes a "restriction" which looks like "f1 \/ f2 \/ ..." where
   * f1, f2 are like "a /\ b /\ ...".
   */
  public Restr<Term> restr(@NotNull Restr<Term> restr) {
    return switch (restr.rename(Function.identity(), this::term)) {
      case Restr.Vary<Term> vary -> {
        var orz = MutableArrayList.<Restr.Cofib<Term>>create(vary.orz().size());
        for (var cof : vary.orz()) {
          // This is a sequence of "or"s, so if any cof is true, the whole thing is true
          if (cofib(cof, orz)) yield new Restr.Const<>(true);
        }
        if (orz.isEmpty()) yield new Restr.Const<>(false);
        yield new Restr.Vary<>(orz.toImmutableArray());
      }
      case Restr.Const<Term> c -> c;
    };
  }

  /**
   * Normalizes a list of "a /\ b /\ ..." into orz.
   * If it is false (implied by any of them being false), orz is unmodified.
   *
   * @return true if this is constantly true
   */
  private boolean cofib(Restr.Cofib<Term> cof, MutableList<Restr.Cofib<Term>> orz) {
    var ands = MutableArrayList.<Restr.Cond<Term>>create(cof.ands().size());
    var localOrz = MutableList.<Formula.Conn<Term>>create();
    if (collectAnds(cof, ands, localOrz)) {
      // Found a false, do not modify orz
      return false;
    }
    if (localOrz.isNotEmpty()) {
      throw new UnsupportedOperationException("TODO");
    }
    if (ands.isNotEmpty()) {
      orz.append(new Restr.Cofib<>(ands.toImmutableArray()));
      return false;
    } else return true;
  }

  /**
   * Only when we cannot simplify an LHS do we add it to "ands".
   * Unsimplifiable terms include negations and non-formulae (e.g. variable references, neutrals, etc.)
   * In case of \/, we add them to "localOrz" and do not add to "ands".
   *
   * @return true if this is constant false
   */
  private boolean collectAnds(Restr.Cofib<Term> cof, MutableList<Restr.Cond<Term>> ands, MutableList<Formula.Conn<Term>> localOrz) {
    var todoAnds = MutableList.from(cof.ands()).asMutableStack();
    while (todoAnds.isNotEmpty()) {
      var and = todoAnds.pop();
      if (and.inst() instanceof Term.Mula mula) switch (mula.formula()) {
        case Formula.Lit<Term> lit -> {
          if (lit.isLeft() != and.isLeft()) return true;
          // Skip truth
        }
        // Do nothing, due to normalization (already done in restr),
        // this must not be a simplifiable involution
        case Formula.Inv<Term> inv -> ands.append(and);
        // a /\ b = 1 ==> a = 1 /\ b = 1
        case Formula.Conn<Term> conn && conn.isAnd() && !and.isLeft() -> {
          todoAnds.push(new Restr.Cond<>(and.i(), conn.l(), false));
          todoAnds.push(new Restr.Cond<>(and.i(), conn.r(), false));
        }
        // a \/ b = 0 ==> a = 0 /\ b = 0
        case Formula.Conn<Term> conn && !conn.isAnd() && and.isLeft() -> {
          todoAnds.push(new Restr.Cond<>(and.i(), conn.l(), true));
          todoAnds.push(new Restr.Cond<>(and.i(), conn.r(), true));
        }
        // a /\ b = 0 ==> a = 0 \/ b = 0
        case Formula.Conn<Term> conn && conn.isAnd() && !and.isLeft() -> localOrz.append(conn);
        // a \/ b = 1 ==> a = 1 \/ b = 1
        case Formula.Conn<Term> conn /*&& !conn.isAnd() && and.isLeft()*/ -> localOrz.append(conn);
      }
      else ands.append(and);
    }
    return false;
  }

  private Term transp(LocalVar i, Term cover, Restr<Term> restr) {
    return switch (cover.app(new Term.Ref(i))) {
      case Term.DT dt && dt.isPi() -> Term.mkLam("f", u0 -> Term.mkLam("x", v -> {
        var laptop = new Transps(rename(new Term.Lam(i, dt.param().type())), restr);
        var w = laptop.invFill(i).app(v);
        // w0 = w.subst(i, 0), according to Minghao Liu
        var w0 = laptop.inv().app(v);
        var cod = rename(new Term.Lam(i, dt.codomain(w)));
        return new Term.Transp(cod, restr).app(u0.app(w0));
      }));
      case Term.UI u && u.isU() -> Term.id("u");
      case Term.DT dt /*&& !dt.isPi()*/ -> Term.mkLam("t", u0 -> {
        var laptop = new Transps(rename(new Term.Lam(i, dt.param().type())), restr);
        // Simon Huber wrote u0.1 both with and without parentheses, extremely confusing!!
        var v = laptop.fill(i).app(u0.proj(true));
        return new Term.Two(false, laptop.mk().app(u0.proj(true)),
          new Term.Transp(rename(new Term.Lam(i, dt.codomain(v))), restr).app(u0.proj(false)));
      });
      default -> new Term.Transp(cover, restr);
    };
  }

  // https://github.com/mortberg/cubicaltt/blob/a5c6f94bfc0da84e214641e0b87aa9649ea114ea/Connections.hs#L178-L197
  private Term formulae(Formula<Term> formula) {
    return switch (formula) { // de Morgan laws
      // ~ 1 = 0, ~ 0 = 1
      case Formula.Inv<Term> inv && inv.i() instanceof Term.Mula i
        && i.formula() instanceof Formula.Lit<Term> lit -> Term.end(!lit.isLeft());
      // ~ (~ a) = a
      case Formula.Inv<Term> inv && inv.i() instanceof Term.Mula i
        && i.formula() instanceof Formula.Inv<Term> ii -> ii.i(); // DNE!! :fear:
      // ~ (a /\ b) = (~ a \/ ~ b), ~ (a \/ b) = (~ a /\ ~ b)
      case Formula.Inv<Term> inv && inv.i() instanceof Term.Mula i
        && i.formula() instanceof Formula.Conn<Term> conn -> new Term.Mula(new Formula.Conn<>(!conn.isAnd(),
        formulae(new Formula.Inv<>(conn.l())),
        formulae(new Formula.Inv<>(conn.r()))));
      // 0 /\ a = 0, 1 /\ a = a, 0 \/ a = a, 1 \/ a = 1
      case Formula.Conn<Term> conn && conn.l() instanceof Term.Mula lf
        && lf.formula() instanceof Formula.Lit<Term> l -> l.isLeft()
        ? (conn.isAnd() ? lf : conn.r())
        : (conn.isAnd() ? conn.r() : lf);
      // a /\ 0 = 0, a /\ 1 = a, a \/ 0 = a, a \/ 1 = 1
      case Formula.Conn<Term> conn && conn.r() instanceof Term.Mula rf
        && rf.formula() instanceof Formula.Lit<Term> r -> r.isLeft()
        ? (conn.isAnd() ? rf : conn.l())
        : (conn.isAnd() ? conn.l() : rf);
      default -> new Term.Mula(formula);
    };
  }

  /** She's buying a stairway to heaven. */
  private @Nullable Term stairway(
    @NotNull Boundary.Data<Term> thoughts,
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
      else if (!(ct._2._2 instanceof Term.Mula mula
        && mula.formula() instanceof Formula.Lit<Term> lit
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
        case Term.Mula f -> new Term.Mula(f.formula().fmap(this::term));
        case Term.Transp transp -> new Term.Transp(term(transp.cover()),
          transp.restr().rename(v -> map.getOrDefault(v, v), this::term));
      };
    }

    private @NotNull Boundary.Data<Term> boundaries(@NotNull Boundary.Data<Term> data) {
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
