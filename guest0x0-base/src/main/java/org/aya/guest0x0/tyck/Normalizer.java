package org.aya.guest0x0.tyck;

import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        var heaven = piper(pApp.b(), i); // Important: use unnormalized pApp.b()
        yield heaven != null ? heaven : new Term.PCall(p, i, pApp.b().fmap(this::term));
      }
      case Term.Mula f -> formulae(f.formula().fmap(this::term));
      case Term.Transp transp -> throw new UnsupportedOperationException("TODO");
    };
  }

  // https://github.com/mortberg/cubicaltt/blob/a5c6f94bfc0da84e214641e0b87aa9649ea114ea/Connections.hs#L178-L197
  private Term formulae(Formula<Term> formula) {
    return switch (formula) { // de Morgan laws
      case Formula.Inv<Term> inv && inv.i() instanceof Term.Mula i
        && i.formula() instanceof Formula.Lit<Term> lit -> Term.end(!lit.isLeft());
      case Formula.Inv<Term> inv && inv.i() instanceof Term.Mula i
        && i.formula() instanceof Formula.Conn<Term> conn -> new Term.Mula(new Formula.Conn<>(!conn.isAnd(),
        formulae(new Formula.Inv<>(conn.l())),
        formulae(new Formula.Inv<>(conn.r()))));
      case Formula.Conn<Term> conn && conn.l() instanceof Term.Mula lf
        && lf.formula() instanceof Formula.Lit<Term> l -> l.isLeft()
        ? (conn.isAnd() ? lf : conn.r())
        : (conn.isAnd() ? conn.r() : lf);
      case Formula.Conn<Term> conn && conn.r() instanceof Term.Mula rf
        && rf.formula() instanceof Formula.Lit<Term> r -> r.isLeft()
        ? (conn.isAnd() ? rf : conn.l())
        : (conn.isAnd() ? conn.l() : rf);
      default -> new Term.Mula(formula);
    };
  }

  /** A piper that will lead us to reason. */
  private @Nullable Term piper(
    @NotNull Boundary.Data<Term> thoughts,
    @NotNull ImmutableSeq<Term> word // With a word she can get what she came for.
  ) {
    assert word.sizeEquals(thoughts.dims().size());
    meaning:
    for (var thought : thoughts.boundaries()) {
      var sign = new Normalizer(sigma, MutableMap.from(rho));
      for (var ct : thought.face().pats().zipView(thoughts.dims().zipView(word))) {
        if (ct._1 == Boundary.Case.VAR) sign.rho.put(ct._2);
        else if (!(ct._2._2 instanceof Term.Mula mula
          && mula.formula() instanceof Formula.Lit<Term> lit
          && lit.isLeft() == (ct._1 == Boundary.Case.LEFT))) continue meaning;
      }
      return sign.term(thought.body());
    }
    return null; // Sometimes all of our thoughts are misgiven.
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
        case Term.Transp transp -> new Term.Transp(term(transp.cover()), transpData(transp.data()));
      };
    }

    private @NotNull Boundary.TranspData transpData(@NotNull Boundary.TranspData data) {
      return new Boundary.TranspData(data.vars().map(v -> map.getOrDefault(v, v)), data.faces());
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
