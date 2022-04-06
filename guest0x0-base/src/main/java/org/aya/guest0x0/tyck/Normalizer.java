package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.*;
import org.jetbrains.annotations.NotNull;

public record Normalizer(
  @NotNull MutableMap<LocalVar, Def<Term>> sigma,
  @NotNull MutableMap<LocalVar, Term> rho
) {
  public Param<Term> param(Param<Term> param) {
    return new Param<>(param.x(), term(param.type()));
  }

  public Term term(Term term) {
    return switch (term) {
      case Term.Ref ref -> rho.getOption(ref.var())
        .map(Renamer::f)
        .map(this::term).getOrDefault(ref);
      case Term.U u -> u;
      case Term.Lam lam -> new Term.Lam(param(lam.param()), term(lam.body()));
      case Term.DT dt -> new Term.DT(dt.isPi(), param(dt.param()), term(dt.cod()));
      case Term.Two two -> {
        var f = term(two.f());
        var a = term(two.a());
        // Either a tuple or a stuck term is preserved
        if (!two.isApp() || !(f instanceof Term.Lam lam)) yield new Term.Two(two.isApp(), f, a);
        rho.put(lam.param().x(), a);
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
    };
  }

  record Renamer(MutableMap<LocalVar, LocalVar> map) {
    public static @NotNull Term f(@NotNull Term term) {
      return new Renamer(MutableMap.create()).term(term);
    }

    public Term term(Term term) {
      return switch (term) {
        case Term.Lam lam -> {
          var param = param(lam.param());
          // Make sure to param before bodying
          yield new Term.Lam(param, term(lam.body()));
        }
        case Term.U u -> u;
        case Term.Ref ref -> map.getOption(ref.var()).map(Term.Ref::new).getOrDefault(ref);
        case Term.DT dt -> {
          var param = param(dt.param());
          // Ditto
          yield new Term.DT(dt.isPi(), param, term(dt.cod()));
        }
        case Term.Two two -> new Term.Two(two.isApp(), term(two.f()), term(two.a()));
        case Term.Proj proj -> new Term.Proj(term(proj.t()), proj.isOne());
        case Term.Call call -> new Term.Call(call.fn(), call.args().map(this::term));
        case Term.Path path -> new Term.Path(path.data().fmap(this::term,
          path.data().dims().map(this::param)));
      };
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
