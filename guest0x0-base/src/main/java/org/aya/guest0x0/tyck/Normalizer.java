package org.aya.guest0x0.tyck;

import kala.collection.mutable.MutableMap;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Term;
import org.jetbrains.annotations.NotNull;

public record Normalizer(@NotNull MutableMap<LocalVar, Term> rho) {
  public static @NotNull Term f(@NotNull Term term) {
    return new Normalizer(MutableMap.create()).term(term);
  }

  public @NotNull Term.Param param(@NotNull Term.Param param) {
    return new Term.Param(param.x(), term(param.type()));
  }

  public @NotNull Term term(@NotNull Term term) {
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
    };
  }

  record Renamer(@NotNull MutableMap<LocalVar, LocalVar> map) {
    public static @NotNull Term f(@NotNull Term term) {
      return new Renamer(MutableMap.create()).term(term);
    }

    public @NotNull Term term(@NotNull Term term) {
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
      };
    }

    private @NotNull Term.Param param(Term.Param param) {
      var var = new LocalVar(param.x().name());
      map.put(param.x(), var);
      return new Term.Param(var, term(param.type()));
    }
  }
}
