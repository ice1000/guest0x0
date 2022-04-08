package org.aya.guest0x0.util;

import kala.collection.Seq;
import kala.collection.mutable.MutableList;
import org.aya.guest0x0.syntax.Boundary;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.Param;
import org.aya.pretty.doc.Doc;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface Distiller {
  static <T> @NotNull Doc boundaryData(@NotNull Boundary.Data<T> data, Function<T, Doc> f) {
    var docs = MutableList.of(Doc.symbol("[|"));
    data.dims().forEach(d -> docs.append(Doc.symbol(d.name())));
    docs.appendAll(new Doc[]{Doc.symbol("|]"), f.apply(data.ty()), Doc.symbol("{")});
    data.boundaries().forEach(b -> {
      docs.append(Doc.symbol("|"));
      b.pats().forEach(d -> docs.append(Doc.symbol(d.name())));
      docs.append(Doc.symbol("=>"));
      docs.append(f.apply(b.body()));
    });
    docs.append(Doc.symbol("}"));
    return Doc.sep(docs);
  }

  static <T> @NotNull Doc param(@NotNull Param<T> data, Function<T, Doc> f) {
    return Doc.parened(Doc.sep(Doc.symbol(data.x().name()), Doc.symbol(":"), f.apply(data.type())));
  }

  static @NotNull Doc expr(@NotNull Expr expr) {
    return switch (expr) {
      case Expr.UI u -> Doc.plain(u.isU() ? "U" : "I");
      case Expr.Two two && two.isApp() -> Doc.parened(Doc.sep(expr(two.f()), expr(two.a())));
      case Expr.Two two /*&& !two.isApp()*/ -> Doc.wrap("<<", ">>",
        Doc.commaList(Seq.of(expr(two.f()), expr(two.a()))));
      case Expr.Lam lam -> Doc.parened(Doc.sep(Doc.cat(
        Doc.plain("\\"), Doc.symbol(lam.x().name()), Doc.plain(".")), expr(lam.a())));
      case Expr.Resolved resolved -> Doc.plain(resolved.ref().name());
      case Expr.Path path -> boundaryData(path.data(), Distiller::expr);
      case Expr.Unresolved unresolved -> Doc.plain(unresolved.name());
      case Expr.Proj proj -> Doc.cat(expr(proj.t()), Doc.plain("." + (proj.isOne() ? 1 : 2)));
      case Expr.DT dt -> Doc.sep(Doc.plain(dt.isPi() ? "Pi" : "Sig"),
        param(dt.param(), Distiller::expr), Doc.symbol(dt.isPi() ? "->" : "**"), expr(dt.cod()));
    };
  }
}
