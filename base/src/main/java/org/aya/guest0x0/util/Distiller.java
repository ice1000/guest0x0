package org.aya.guest0x0.util;

import kala.collection.Seq;
import kala.collection.SeqView;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.cubical.Formula;
import org.aya.guest0x0.cubical.Restr;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Term;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static org.aya.guest0x0.util.Distiller.Prec.*;

public interface Distiller {
  @FunctionalInterface
  interface PP<E> extends BiFunction<E, Prec, Doc> {}
  enum Prec {
    Free, IOp, Cod, AppHead, AppSpine, ProjHead
  }
  static @NotNull Doc expr(@NotNull Expr expr, Prec envPrec) {
    return switch (expr) {
      case Expr.PrimTy u -> Doc.plain(u.keyword().name());
      case Expr.Two two && two.isApp() -> appPrec(envPrec, Doc.sep(expr(two.f(), AppHead), expr(two.a(), AppSpine)));
      case Expr.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(expr(two.f(), Free), expr(two.a(), Free))));
      case Expr.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.x().name()), Doc.plain(".")), expr(lam.a(), Free));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.Resolved resolved -> Doc.plain(resolved.ref().name());
      case Expr.Path path -> path.data().toDoc();
      case Expr.Unresolved unresolved -> Doc.plain(unresolved.name());
      case Expr.Proj proj -> Doc.cat(expr(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Expr.DT dt -> {
        var doc = dependentType(dt.isPi(), dt.param(), expr(dt.cod(), Cod));
        yield envPrec.ordinal() > Cod.ordinal() ? Doc.parened(doc) : doc;
      }
      case Expr.Hole ignored -> Doc.symbol("_");
      case Expr.Mula e -> formulae(Distiller::expr, e.asFormula(), envPrec);
      case Expr.Transp transp -> fibred("tr", expr(transp.cover(), AppSpine), transp.restr());
      // Well, hopefully I guessed the precedence right.
      case Expr.Cof cof -> appPrec(envPrec, cof.data().toDoc());
      case Expr.PartEl par -> Doc.sep(Doc.symbol("\\"), partial(par));
      case Expr.PartTy par -> fibred("Partial", expr(par.ty(), AppSpine), par.restr());
      case Expr.Sub sub -> Doc.sep(Doc.plain("Sub"),
        expr(sub.ty(), Free), partial(sub.par()));
      case Expr.SubEl subEl ->
        appPrec(envPrec, Doc.sep(Doc.plain(subEl.isIntro() ? "inS" : "outS"), expr(subEl.e(), AppSpine)));
      case Expr.Hcomp hcomp -> appPrec(envPrec, hcomp.data().toDoc());
    };
  }
  private static Doc appPrec(Prec envPrec, Doc doc) {
    return envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
  }
  @NotNull private static Doc partial(Expr.PartEl par) {
    return Doc.wrap("[|", "|]",
      Doc.join(Doc.symbol("|"), clauses(par.clauses())));
  }
  private static @NotNull Doc fibred(String kw, Doc cover, Docile restr) {
    return Doc.sep(Doc.plain(kw), cover.toDoc(),
      Doc.symbol("#{"), restr.toDoc(), Doc.symbol("}"));
  }
  private static @NotNull Doc dependentType(boolean isPi, Param<?> param, Docile cod) {
    return Doc.sep(Doc.plain(isPi ? "Pi" : "Sig"),
      param.toDoc(), Doc.symbol(isPi ? "->" : "**"), cod.toDoc());
  }
  private static @NotNull <E> Doc formulae(PP<E> f, Formula<E> formula, Prec envPrec) {
    var doc = switch (formula) {
      case Formula.Conn<E> conn -> Doc.sep(f.apply(conn.l(), IOp),
        Doc.symbol(conn.isAnd() ? "/\\" : "\\/"), f.apply(conn.r(), IOp));
      case Formula.Inv<E> inv -> Doc.sep(Doc.plain("~"), f.apply(inv.i(), IOp));
      case Formula.Lit<E> lit -> {
        envPrec = Free; // A hack to force no paren
        yield Doc.symbol(lit.isLeft() ? "0" : "1");
      }
    };
    return envPrec.ordinal() >= IOp.ordinal() ? Doc.parened(doc) : doc;
  }

  static @NotNull Doc term(@NotNull Term term, Prec envPrec) {
    return switch (term) {
      case Term.DT dt -> {
        var doc = dependentType(dt.isPi(), dt.param(), term(dt.cod(), Cod));
        yield envPrec.ordinal() > Cod.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.UI ui -> Doc.plain(ui.keyword().name());
      case Term.Ref ref -> Doc.plain(ref.var().name());
      case Term.Path path -> path.data().toDoc();
      case Term.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.x().name()), Doc.plain(".")), term(lam.body(), Free));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Proj proj -> Doc.cat(term(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Term.Two two && two.isApp() -> appPrec(envPrec, Doc.sep(term(two.f(), AppHead), term(two.a(), AppSpine)));
      case Term.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f(), Free), term(two.a(), Free))));
      case Term.Call call -> appPrec(envPrec, Doc.sep(call.args().view()
        .map(t -> term(t, AppSpine)).prepended(Doc.plain(call.fn().name()))));
      case Term.PCall call -> appPrec(envPrec, Doc.sep(call.i().view()
        .map(t -> term(t, AppSpine)).prepended(term(call.p(), AppHead))));
      case Term.PLam pLam -> {
        var docs = MutableList.of(Doc.plain("\\"));
        pLam.dims().forEach(d -> docs.append(Doc.symbol(d.name())));
        docs.append(Doc.plain("."));
        docs.append(term(pLam.fill(), Free));
        var doc = Doc.sep(docs);
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Mula f -> formulae(Distiller::term, f.asFormula(), envPrec);
      case Term.Transp transp -> fibred("tr", term(transp.cover(), AppSpine), transp.restr());
      case Term.Cof cof -> appPrec(envPrec, cof.restr().toDoc());
      case Term.PartTy par -> fibred("Partial", term(par.ty(), AppSpine), par.restr());
      case Term.PartEl par -> partial(par);
      case Term.Sub sub -> Doc.sep(Doc.plain("Sub"), term(sub.ty(), Free), partial(sub.par()));
      case Term.InS inS -> Doc.sep(Doc.plain("inS"), term(inS.e(), Free));
      case Term.OutS outS -> Doc.sep(Doc.plain("outS"), term(outS.e(), AppSpine));
    };
  }
  private static @NotNull Doc partial(Term.PartEl par) {
    return switch (par) {
      case Term.ReallyPartial partial -> Doc.wrap("[|", "|]",
        Doc.join(Doc.symbol("|"), clauses(partial.clauses())));
      case Term.SomewhatPartial partial -> Doc.wrap("[|", "|]",
        term(partial.obvious(), Free));
    };
  }
  static <T extends Restr.TermLike<T>> SeqView<Doc> clauses(@NotNull Seq<Restr.Side<T>> clauses) {
    return clauses.view()
      .map(Restr.Side::toDoc)
      .map(Doc::spaced);
  }
}
