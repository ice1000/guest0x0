package org.aya.guest0x0.tyck;

import kala.collection.Seq;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableArrayList;
import kala.collection.mutable.MutableList;
import kala.collection.mutable.MutableMap;
import kala.control.Option;
import kala.tuple.Tuple;
import org.aya.guest0x0.cubical.CofThy;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.BdryData;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.guest0x0.util.AltF7;
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
      case Expr.Lam lam -> switch (normalize(type)) {
        case Term.DT dt && dt.isPi() -> new Term.Lam(lam.x(),
          hof(lam.x(), dt.param().type(), () -> inherit(lam.a(), dt.codomain(new Term.Ref(lam.x())))));
        // Overloaded lambda
        case Term.Path path -> {
          var tyDims = path.data().dims();
          var lamDims = MutableArrayList.<LocalVar>create(tyDims.size());
          var unlam = Expr.unlam(lamDims, tyDims.size(), lam);
          if (unlam == null) throw new SPE(lam.pos(), Doc.english("Expected path lambda"));
          yield boundaries(lamDims, () -> inherit(unlam,
            normalizer(lamDims, tyDims).term(path.data().type())
          ), unlam.pos(), path.data(), normalizer(tyDims, lamDims));
        }
        default -> throw new SPE(lam.pos(),
          Doc.english("Expects a right adjoint for"), expr, Doc.plain("got"), type);
      };
      case Expr.PartEl el -> {
        if (!(normalize(type) instanceof Term.PartTy par)) throw new SPE(el.pos(),
          Doc.english("Expects partial type for partial elements"), expr, Doc.plain("got"), type);
        var cof = cof(par.restr(), el.pos());
        var clauses = elaborateClauses(el, el.clauses(), par.ty());
        var face = restrOfClauses(clauses);
        if (!CofThy.conv(cof.restr(), Normalizer.create(), norm -> CofThy.satisfied(norm.restr(face))))
          throw new SPE(el.pos(), Doc.english("The faces in the partial element"), face,
            Doc.english("must cover the face(s) specified in type:"), cof);
        yield new Term.ReallyPartial(clauses);
      }
      case Expr.Two two && !two.isApp() -> {
        if (!(normalize(type) instanceof Term.DT dt) || dt.isPi()) throw new SPE(two.pos(),
          Doc.english("Expects a left adjoint for"), expr, Doc.plain("got"), type);
        var lhs = inherit(two.f(), dt.param().type());
        yield new Term.Two(false, lhs, inherit(two.a(), dt.codomain(lhs)));
      }
      case Expr.SubEl inS && inS.isIntro() -> {
        if (!(normalize(type) instanceof Term.Sub sub)) throw new SPE(inS.pos(),
          Doc.english("Expects cubical subtype, got"), type);
        var arg = inherit(inS.e(), sub.ty());
        switch (sub.par()) {
          case Term.ReallyPartial partial -> boundaries(inS.pos(), normalizer(), arg, partial.clauses());
          case Term.SomewhatPartial partial -> unify(arg, inS, partial.obvious(), inS.pos());
        }
        yield new Term.InS(arg, sub.par().restr());
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
        yield switch (normalize(type)) {
          case Term.Path path -> {
            var exTyDims = path.data().dims();
            var lamDims = MutableArrayList.<LocalVar>create(exTyDims.size());
            var unlam = Term.unlam(lamDims, synth.wellTyped, exTyDims.size());
            var acTyDims = MutableArrayList.<LocalVar>create(exTyDims.size());
            var unpi = Term.unpi(acTyDims, synth.type, exTyDims.size());
            if (unlam == null || unpi == null) throw new SPE(expr.pos(), Doc.english("Expected (path) lambda"));
            unify(unpi, unlam, normalizer(exTyDims, acTyDims).term(path.data().type()), expr.pos());
            yield boundaries(lamDims, () -> unlam, expr.pos(), path.data(), normalizer(exTyDims, lamDims));
          }
          case Term ty -> {
            unify(ty, synth.wellTyped, synth.type, expr.pos());
            yield synth.wellTyped;
          }
        };
      }
    };
  }

  public static @NotNull Restr<Term> restrOfClauses(ImmutableSeq<Restr.Side<Term>> clauses) {
    return new Term.ReallyPartial(clauses).restr();
  }

  private Normalizer normalizer(Seq<LocalVar> from, Seq<LocalVar> to) {
    return new Normalizer(sigma, MutableMap.from(
      from.zipView(to).map(t -> Tuple.of(t._1, new Term.Ref(t._2)))
    ));
  }

  private ImmutableSeq<Restr.Side<Term>> elaborateClauses(
    Expr on, @NotNull ImmutableSeq<Restr.Side<Expr>> clauses, @NotNull Term ty
  ) {
    var sides = clauses.flatMap(cl -> clause(on.pos(), cl, ty));
    confluence(on, sides, on.pos());
    return sides;
  }

  private void confluence(Docile on, ImmutableSeq<Restr.Side<Term>> clauses, @NotNull SourcePos pos) {
    for (int i = 1; i < clauses.size(); i++) {
      var lhs = clauses.get(i);
      for (int j = 0; j < i; j++) {
        var rhs = clauses.get(j);
        CofThy.conv(lhs.cof().and(rhs.cof()), normalizer(), norm -> {
          unify(norm.term(lhs.u()), on, norm.term(rhs.u()), pos, Doc.english("Boundaries disagree."));
          return true;
        });
      }
    }
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos, Doc prefix) {
    unify(ty, actual, pos, u -> Doc.vcat(prefix, unifyDoc(ty, on, actual, u)));
  }

  private void unify(Term ty, Docile on, @NotNull Term actual, SourcePos pos) {
    unify(ty, actual, pos, u -> unifyDoc(ty, on, actual, u));
  }

  private static void unify(Term ty, Term actual, SourcePos pos, Function<Unifier, Doc> message) {
    var unifier = new Unifier();
    if (!unifier.untyped(actual, ty))
      throw new SPE(pos, message.apply(unifier));
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

  /**
   * @param subst        Must be purely variable-to-variable
   * @param coreSupplier Already substituted with the variables in the type
   * @param lamDims      Bindings in the type
   */
  private @NotNull Term boundaries(
    @NotNull MutableList<LocalVar> lamDims,
    @NotNull Supplier<Term> coreSupplier,
    SourcePos pos, BdryData<Term> data,
    Normalizer subst
  ) {
    lamDims.forEach(t -> gamma.put(t, Term.I));
    var core = coreSupplier.get();
    boundaries(pos, subst, core, data.boundaries());
    lamDims.forEach(gamma::remove);
    return new Term.PLam(lamDims.toImmutableArray(), core);
  }

  private void boundaries(SourcePos pos, Normalizer subst, Term core, ImmutableSeq<Restr.Side<Term>> boundaries) {
    for (var boundary : boundaries) {
      // Based on the very assumption as in the function's javadoc
      CofThy.conv(boundary.cof().fmap(subst::term), subst, norm -> {
        unify(norm.term(boundary.u()), boundary, norm.term(core), pos,
          Doc.english("Boundary mismatch, oh no."));
        return true;
      });
    }
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
      case Expr.SubEl outS && !outS.isIntro() -> {
        var e = synth(outS.e());
        if (!(e.type instanceof Term.Sub sub))
          throw new SPE(outS.pos(), Doc.english("Expects a cubical subtype, got"), e.type);
        yield new Synth(new Term.OutS(e.wellTyped, sub.par()), sub.ty());
      }
      case Expr.Sub sub -> {
        var ty = inherit(sub.ty(), Term.U);
        var clauses = elaborateClauses(sub, sub.par().clauses(), ty);
        yield new Synth(new Term.Sub(ty, new Term.ReallyPartial(clauses)), Term.U);
      }
      case Expr.Path path -> {
        var dims = path.data().dims();
        for (var dim : dims) gamma.put(dim, Term.I);
        var ty = inherit(path.data().type(), Term.U);
        var boundaries = elaborateClauses(expr, path.data().boundaries(), ty);
        var data = new BdryData<>(dims, ty, boundaries);
        for (var dim : dims) gamma.remove(dim);
        yield new Synth(new Term.Path(data), Term.U);
      }
      case Expr.Mula f -> switch (f.asFormula()) {
        case Formula.Inv<Expr> inv -> new Synth(Term.neg(inherit(inv.i(), Term.I)), Term.I);
        case Formula.Conn<Expr> conn -> new Synth(new Term.Mula(
          new Formula.Conn<>(conn.isAnd(), inherit(conn.l(), Term.I), inherit(conn.r(), Term.I))), Term.I);
        case Formula.Lit<Expr> lit -> new Synth(Term.end(lit.isLeft()), Term.I);
      };
      case Expr.Cof cof -> new Synth(new Term.Cof(cof.data().mapCond(this::condition)), Term.F);
      case Expr.PartTy par -> new Synth(new Term.PartTy(inherit(par.ty(), Term.U), cof(par.restr())), Term.U);
      case Expr.Transp transp -> {
        var cover = inherit(transp.cover(), Term.mkPi(Term.I, Term.U));
        var detective = new AltF7(new LocalVar("?"));
        var sample = cover.app(new Term.Ref(detective.var()));
        var ty = Term.mkPi(cover.app(Term.end(true)), cover.app(Term.end(false)));
        var cof = cof(transp.restr());
        // I believe find-usages is slightly more efficient than what Huber wrote in hcomp.pdf
        var capture = new Object() {
          Term under = sample;
        };
        // I want it to have no references, so !detective.press(), and if it returns true, it means no problem
        // So if it returns false then we're in trouble
        if (!CofThy.conv(cof.restr(), normalizer(), n -> !detective.press(capture.under = n.term(sample))))
          throw new SPE(transp.pos(), Doc.english("The cover"), cover,
            Doc.english("has to be constant under the cofibration"), cof,
            Doc.english("but applying a variable `?` to it results in"), capture.under,
            Doc.english("which contains a reference to `?`, oh no"));
        yield new Synth(new Term.Transp(cover, cof), ty);
      }
      default -> throw new SPE(expr.pos(), Doc.english("Synthesis failed for"), expr);
    };
    var type = normalize(synth.type);
    if (type instanceof Term.Path path) {
      var dims = path.data().dims();
      return new Synth(Normalizer.rename(Term.mkLam(dims.view(),
        new Term.PCall(synth.wellTyped, dims.map(Term.Ref::new), new Term.ReallyPartial(path.data().boundaries())))),
        Term.mkPi(dims.map(x -> new Param<>(x, Term.I)), path.data().type()));
    } else return new Synth(synth.wellTyped, type);
  }

  private @NotNull Restr.Cond<Term> condition(Restr.Cond<Expr> c) {
    return new Restr.Cond<>(inherit(c.inst(), Term.I), c.isLeft());
  }

  private @NotNull Term.Cof cof(@NotNull Expr restr) {
    return cof(inherit(restr, Term.F), restr.pos());
  }

  private static @NotNull Term.Cof cof(Term restr, @NotNull SourcePos pos) {
    if (!(restr instanceof Term.Cof cof))
      throw new SPE(pos, Doc.english("Expects a cofibration literal, got"), restr);
    return cof;
  }

  private @NotNull Option<Restr.Side<Term>> clause(
    @NotNull SourcePos pos,
    @NotNull Restr.Side<Expr> clause, @NotNull Term ty
  ) {
    var cofib = new Restr.Cofib<>(clause.cof().ands().map(this::condition));
    var u = CofThy.vdash(cofib, normalizer(), norm -> inherit(clause.u(), norm.term(ty)));
    if (u.isDefined() && u.get() == null)
      throw new SPE(pos, Doc.english("The cofibration in"), cofib,
        Doc.english("is not well-defined"));
    return u.map(uu -> new Restr.Side<>(cofib, uu));
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
