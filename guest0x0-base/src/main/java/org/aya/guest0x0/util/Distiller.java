package org.aya.guest0x0.util;

import kala.collection.Seq;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Param;
import org.aya.guest0x0.syntax.Term;
import org.aya.pretty.doc.Doc;
import org.aya.pretty.doc.Docile;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

public interface Distiller {
  int FREE = 0, CODOMAIN = 1, APP_HEAD = 2, APP_SPINE = 3, PROJ_HEAD = 4;
  static @NotNull Doc expr(@NotNull Expr expr, @MagicConstant(valuesFromClass = Distiller.class) int envPrec) {
    return switch (expr) {
      case Expr.UI u -> Doc.plain(u.isU() ? "U" : "I");
      case Expr.Two two && two.isApp() -> {
        var inner = Doc.sep(expr(two.f(), APP_HEAD), expr(two.a(), APP_SPINE));
        yield envPrec > APP_HEAD ? Doc.parened(inner) : inner;
      }
      case Expr.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(expr(two.f(), FREE), expr(two.a(), FREE))));
      case Expr.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.x().name()), Doc.plain(".")), expr(lam.a(), FREE));
        yield envPrec > FREE ? Doc.parened(doc) : doc;
      }
      case Expr.Resolved resolved -> Doc.plain(resolved.ref().name());
      case Expr.Path path -> path.data().toDoc();
      case Expr.Unresolved unresolved -> Doc.plain(unresolved.name());
      case Expr.Proj proj -> Doc.cat(expr(proj.t(), PROJ_HEAD), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Expr.DT dt -> {
        var doc = dependentType(dt.isPi(), dt.param(), expr(dt.cod(), CODOMAIN));
        yield envPrec > CODOMAIN ? Doc.parened(doc) : doc;
      }
    };
  }
  private static @NotNull Doc dependentType(boolean isPi, Param<?> param, Docile cod) {
    return Doc.sep(Doc.plain(isPi ? "Pi" : "Sig"),
      param.toDoc(), Doc.symbol(isPi ? "->" : "**"), cod.toDoc());
  }

  static @NotNull Doc term(@NotNull Term term, int envPrec) {
    return switch (term) {
      case Term.DT dt -> {
        var doc = dependentType(dt.isPi(), dt.param(), term(dt.cod(), CODOMAIN));
        yield envPrec > CODOMAIN ? Doc.parened(doc) : doc;
      }
      case Term.UI ui -> Doc.plain(ui.isU() ? "U" : "I");
      case Term.Ref ref -> Doc.plain(ref.var().name());
      case Term.Path path -> path.data().toDoc();
      case Term.Lam lam -> {
        var doc = Doc.sep(Doc.cat(Doc.plain("\\"),
          Doc.symbol(lam.param().x().name()), Doc.plain(".")), term(lam.body(), FREE));
        yield envPrec > FREE ? Doc.parened(doc) : doc;
      }
      case Term.Proj proj -> Doc.cat(term(proj.t(), PROJ_HEAD), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Term.End end -> Doc.plain(end.isLeft() ? "0" : "1");
      case Term.Two two && two.isApp() -> {
        var inner = Doc.sep(term(two.f(), APP_HEAD), term(two.a(), APP_SPINE));
        yield envPrec > APP_HEAD ? Doc.parened(inner) : inner;
      }
      case Term.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(term(two.f(), FREE), term(two.a(), FREE))));
      case Term.Call call -> {
        var doc = Doc.parened(Doc.sep(call.args().view()
          .map(t -> term(t, APP_SPINE)).prepended(Doc.plain(call.fn().name()))));
        yield envPrec > APP_HEAD ? Doc.parened(doc) : doc;
      }
      case Term.PApp pApp -> term(new Term.Two(true, pApp.p(), pApp.i()), envPrec);
      case Term.PLam pLam -> {
        var docs = MutableList.of(Doc.plain("\\"));
        pLam.dims().forEach(d -> docs.append(Doc.symbol(d.name())));
        docs.append(Doc.plain("."));
        docs.append(term(pLam.fill(), FREE));
        yield Doc.parened(Doc.sep(docs));
      }
    };
  }
}