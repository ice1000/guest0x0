package org.aya.guest0x0.util;

import kala.collection.Seq;
import kala.collection.immutable.ImmutableSeq;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.syntax.*;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

import static org.aya.guest0x0.util.Distiller.Prec.*;

public interface Distiller {
  @FunctionalInterface
  interface PP<E> extends BiFunction<E, Prec, Doc> {}
  enum Prec {
    Free, IOp, Transp, Cod, AppHead, AppSpine, ProjHead
  }
  static @NotNull Doc expr(@NotNull Expr expr, Prec envPrec) {
    return switch (expr) {
      case Expr.UI u -> Doc.plain(u.isU() ? "U" : "I");
      case Expr.Two two && two.isApp() -> {
        var inner = Doc.sep(expr(two.f(), AppHead), expr(two.a(), AppSpine));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(inner) : inner;
      }
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
      case Expr.Formula e -> formulae(Distiller::expr, e.formula(), envPrec);
      case Expr.Transp transp -> transp(Distiller::expr, envPrec, transp.cover(), transp.vars(), transp.faces());
    };
  }
  private static <E> @NotNull Doc transp(
    PP<E> f, Prec envPrec, E cover,
    ImmutableSeq<LocalVar> lv, ImmutableSeq<Boundary.Face> faces
  ) {
    var pre = Doc.sep(f.apply(cover, Transp), Doc.plain("~@"),
      Doc.sep(lv.map(v -> Doc.plain(v.name()))));
    var doc = Doc.cblock(pre, 2, Doc.vcat(faces.map(Boundary.Face::toDoc)));
    return envPrec.ordinal() >= Transp.ordinal() ? Doc.parened(doc) : doc;
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
      case Term.UI ui -> Doc.plain(ui.isU() ? "U" : "I");
      case Term.Ref ref -> Doc.plain(ref.var().name());
      case Term.Path path -> path.data().toDoc();
      case Term.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.x().name()), Doc.plain(".")), term(lam.body(), Free));
        yield envPrec.ordinal() > Free.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.Proj proj -> Doc.cat(term(proj.t(), ProjHead), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Term.Two two && two.isApp() -> {
        var inner = Doc.sep(term(two.f(), AppHead), term(two.a(), AppSpine));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(inner) : inner;
      }
      case Term.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f(), Free), term(two.a(), Free))));
      case Term.Call call -> {
        var doc = Doc.sep(call.args().view()
          .map(t -> term(t, AppSpine)).prepended(Doc.plain(call.fn().name())));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.PCall call -> {
        var doc = Doc.sep(call.i().view()
          .map(t -> term(t, AppSpine)).prepended(term(call.p(), AppHead)));
        yield envPrec.ordinal() > AppHead.ordinal() ? Doc.parened(doc) : doc;
      }
      case Term.PLam pLam -> {
        var docs = MutableList.of(Doc.plain("\\"));
        pLam.dims().forEach(d -> docs.append(Doc.symbol(d.name())));
        docs.append(Doc.plain("."));
        docs.append(term(pLam.fill(), Free));
        yield Doc.parened(Doc.sep(docs));
      }
      case Term.Formula f -> formulae(Distiller::term, f.formula(), envPrec);
      case Term.Transp transp -> transp(Distiller::term, envPrec, transp.cover(), transp.vars(), transp.faces());
    };
  }
}
