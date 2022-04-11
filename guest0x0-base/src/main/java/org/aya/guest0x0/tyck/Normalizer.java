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
      case Term.End end -> end;
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
      case Term.Inv inv -> new Term.Inv(term(inv.i()));
      case Term.Conn conn -> new Term.Conn(conn.isAnd(), term(conn.l()), term(conn.r()));
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
      for (var ct : thought.pats().zipView(thoughts.dims().zipView(word))) {
        if (ct._1 == Boundary.Case.VAR) sign.rho.put(ct._2);
        else if (!(ct._2._2 instanceof Term.End end && end.isLeft() == (ct._1 == Boundary.Case.LEFT))) continue meaning;
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
        case Term.End end -> end;
        case Term.Ref ref -> map.getOption(ref.var()).map(Term.Ref::new).getOrDefault(ref);
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
        case Term.Inv inv -> new Term.Inv(term(inv.i()));
        case Term.Conn conn -> new Term.Conn(conn.isAnd(), term(conn.l()), term(conn.r()));
      };
    }

    private @NotNull Boundary.Data<Term> boundaries(Boundary.@NotNull Data<Term> data) {
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
