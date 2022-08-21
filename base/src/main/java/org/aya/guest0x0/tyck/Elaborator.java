package org.aya.guest0x0.tyck;

import kala.collection.Seq;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.tuple.Tuple;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.LocalVar;
import org.aya.guest0x0.util.Param;
import org.aya.guest0x0.util.SPE;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public record Elaborator(
  @NotNull MutableMap<LocalVar, Def<Term>> sigma,
  @NotNull MutableMap<LocalVar, Term> gamma
) {
  @NotNull public Term normalize(@NotNull Term term) {
    return normalizer().term(term);
  }

  private @NotNull Normalizer normalizer() {
    return new Normalizer(sigma, MutableMap.create());
  }

  public record Synth(@NotNull Term wellTyped, @NotNull Term type) {}

  public Term inherit(Expr expr, Term type) {
    return switch (expr) {
      case Expr.Lam lam -> {
        if (normalize(type) instanceof Term.DT dt && dt.isPi())
          yield new Term.Lam(lam.x(), hof(lam.x(), dt.param().type(), () ->
            inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())))));
        else throw new SPE(lam.pos(),
          Doc.english("Expects a right adjoint for"), expr, Doc.plain("got"), type);
      }
      case Expr.Two two && !two.isApp() -> {
        if (!(normalize(type) instanceof Term.DT dt) || dt.isPi()) throw new SPE(two.pos(),
          Doc.english("Expects a left adjoint for"), expr, Doc.plain("got"), type);
        var lhs = inherit(two.f(), dt.param().type());
        yield new Term.Two(false, lhs, inherit(two.a(), dt.codomain(lhs)));
      }
      case Expr.Hole hole -> {
        var docs = MutableList.<Doc>create();
        gamma.forEach((k, v) -> {
          var list = MutableList.of(Doc.plain(k.name()));
          if (!hole.accessible().contains(k)) list.append(Doc.english("(out of scope)"));
          list.appendAll(new Doc[]{Doc.symbol(":"), normalize(v).toDoc()});
          docs.append(Doc.sep(list));
        });
        docs.append(Doc.plain("----------------------------------"));
        var tyDoc = type.toDoc();
        docs.append(tyDoc);
        var normDoc = normalize(type).toDoc();
        if (!tyDoc.equals(normDoc)) {
          docs.append(Doc.symbol("|->"));
          docs.append(normDoc);
        }
        throw new SPE(hole.pos(), Doc.vcat(docs));
      }
      default -> {
        var synth = synth(expr);
        unify(normalize(type), synth.wellTyped, synth.type, expr.pos());
        yield synth.wellTyped;
      }
    };
  }

  private Normalizer normalizer(Seq<LocalVar> from, Seq<LocalVar> to) {
    return new Normalizer(sigma, MutableMap.from(
      from.zipView(to).map(t -> Tuple.of(t._1, new Term.Ref(t._2)))
    ));
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos, Doc prefix) {
    unify(ty, actual, pos, u -> Doc.vcat(prefix, unifyDoc(ty, on, actual, u)));
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos) {
    unify(ty, actual, pos, u -> unifyDoc(ty, on, actual, u));
  }

  private static void unify(Term ty, Term actual, SourcePos pos, Function<Unifier, Doc> message) {
    var unifier = new Unifier();
    if (!unifier.untyped(actual, ty)) throw new SPE(pos, message.apply(unifier));
  }

  private static @NotNull Doc unifyDoc(Docile ty, Docile on, Docile actual, Unifier unifier) {
    var line1 = Doc.sep(Doc.plain("Umm,"), ty.toDoc(), Doc.plain("!="),
      actual.toDoc(), Doc.english("on"), on.toDoc());
    if (unifier.data != null) {
      var line2 = Doc.sep(Doc.english("In particular,"),
        unifier.data.l().toDoc(), Doc.symbol("!="), unifier.data.r().toDoc());
      line1 = Doc.vcat(line1, line2);
    }
    return line1;
  }

  public Synth synth(Expr expr) {
    var synth = switch (expr) {
      case Expr.PrimTy u -> new Synth(new Term.UI(u.keyword()), Term.U);
      case Expr.Resolved resolved -> {
        var type = gamma.getOrNull(resolved.ref());
        if (type != null) yield new Synth(new Term.Ref(resolved.ref()), type);
        var def = sigma.get(resolved.ref());
        var pi = Term.mkPi(def.telescope(), def.result());
        yield switch (def) {
          case Def.Fn<Term> fn -> new Synth(Normalizer.rename(Term.mkLam(
            fn.telescope().view().map(Param::x), fn.body())), pi);
          case Def.Print<Term> print -> throw new AssertionError("unreachable: " + print);
        };
      }
      case Expr.Proj proj -> {
        var t = synth(proj.t());
        if (!(t.type instanceof Term.DT dt) || dt.isPi())
          throw new SPE(proj.pos(), Doc.english("Expects a left adjoint, got"), t.type);
        var fst = t.wellTyped.proj(true);
        if (proj.isOne()) yield new Synth(fst, dt.param().type());
        yield new Synth(t.wellTyped.proj(false), dt.codomain(fst));
      }
      case Expr.Two two -> {
        var f = synth(two.f());
        if (two.isApp()) {
          if (!(f.type instanceof Term.DT dt) || !dt.isPi())
            throw new SPE(two.pos(), Doc.english("Expects pi, got"), f.type, Doc.plain("when checking"), two);
          var a = hof(dt.param().x(), dt.param().type(), () -> inherit(two.a(), dt.param().type()));
          yield new Synth(f.wellTyped.app(a), dt.codomain(a));
        } else {
          var a = synth(two.a());
          yield new Synth(new Term.Two(false, f.wellTyped, a.wellTyped),
            new Term.DT(false, new Param<>(new LocalVar("_"), f.type), a.type));
        }
      }
      case Expr.DT dt -> {
        var param = synth(dt.param().type());
        var x = dt.param().x();
        var cod = hof(x, param.wellTyped, () -> synth(dt.cod()));
        yield new Synth(new Term.DT(dt.isPi(), new Param<>(x, param.wellTyped), cod.wellTyped), cod.type);
      }
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    return new Synth(synth.wellTyped, type);
  }

  private <T> T hof(@NotNull LocalVar x, @NotNull Term type, @NotNull Supplier<T> t) {
    gamma.put(x, type);
    var ok = t.get();
    gamma.remove(x);
    return ok;
  }

  public Def<Term> def(Def<Expr> def) {
    var telescope = telescope(def);
    var result = inherit(def.result(), Term.U);
    return switch (def) {
      case Def.Fn<Expr> fn -> {
        var body = inherit(fn.body(), result);
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Fn<>(def.name(), telescope, result, body);
      }
      case Def.Print<Expr> print -> {
        var body = inherit(print.body(), result);
        telescope.forEach(key -> gamma.remove(key.x()));
        yield new Def.Print<>(telescope, result, body);
      }
    };
  }

  private @NotNull ImmutableSeq<Param<Term>> telescope(Def<Expr> def) {
    var telescope = MutableArrayList.<Param<Term>>create(def.telescope().size());
    for (var param : def.telescope()) {
      var ty = inherit(param.type(), Term.U);
      telescope.append(new Param<>(param.x(), ty));
      gamma.put(param.x(), ty);
    }
    return telescope.toImmutableArray();
  }
}
