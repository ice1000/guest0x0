package org.aya.guest0x0.cli;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.antlr.v4.runtime.ParserRuleContext;
import org.aya.guest0x0.parser.Guest0x0Parser;
import org.aya.guest0x0.syntax.Def;
import org.aya.guest0x0.syntax.Expr;
import org.aya.guest0x0.syntax.LocalVar;
import org.aya.guest0x0.syntax.Param;
import org.aya.util.error.SourceFile;
import org.aya.util.error.SourcePos;
import org.jetbrains.annotations.NotNull;

public record Parser(@NotNull SourceFile source) {
  public @NotNull Expr expr(@NotNull Guest0x0Parser.ExprContext expr) {
    return switch (expr) {
      case Guest0x0Parser.ParenContext paren -> expr(paren.expr());
      case Guest0x0Parser.TwoContext app -> new Expr.Two(true, sourcePosOf(app), expr(app.expr(0)), expr(app.expr(1)));
      case Guest0x0Parser.PairContext p -> new Expr.Two(false, sourcePosOf(p), expr(p.expr(0)), expr(p.expr(1)));
      case Guest0x0Parser.FstContext fst -> new Expr.Proj(sourcePosOf(fst), expr(fst.expr()), true);
      case Guest0x0Parser.SndContext snd -> new Expr.Proj(sourcePosOf(snd), expr(snd.expr()), false);
      case Guest0x0Parser.TreborContext trebor -> new Expr.Trebor(sourcePosOf(trebor));
      case Guest0x0Parser.LamContext lam -> buildLam(sourcePosOf(lam),
        ImmutableSeq.from(lam.ID()).view().map(id -> new LocalVar(id.getText())), expr(lam.expr()));
      case Guest0x0Parser.RefContext ref -> new Expr.Unresolved(sourcePosOf(ref), ref.ID().getText());
      case Guest0x0Parser.PiContext pi -> buildDT(true, sourcePosOf(pi), param(pi.param()), expr(pi.expr()));
      case Guest0x0Parser.SigContext si -> buildDT(false, sourcePosOf(si), param(si.param()), expr(si.expr()));
      case Guest0x0Parser.SimpFunContext pi -> new Expr.DT(true, sourcePosOf(pi), param(pi.expr(0)), expr(pi.expr(1)));
      case Guest0x0Parser.SimpTupContext si -> new Expr.DT(false, sourcePosOf(si), param(si.expr(0)), expr(si.expr(1)));
      default -> throw new IllegalArgumentException("Unknown expr: " + expr.getClass().getName());
    };
  }

  public @NotNull Def<Expr> def(@NotNull Guest0x0Parser.DeclContext decl) {
    return switch (decl) {
      case Guest0x0Parser.FnDeclContext def -> new Def.Fn<>(
        new LocalVar(def.ID().getText()),
        ImmutableSeq.from(def.param()).flatMap(this::param),
        expr(def.expr(0)),
        expr(def.expr(1)));
      default -> throw new IllegalArgumentException("Unknown def: " + decl.getClass().getName());
    };
  }

  private static Expr buildDT(boolean isPi, SourcePos sourcePos, SeqView<Param<Expr>> params, Expr body) {
    if (params.isEmpty()) return body;
    return new Expr.DT(isPi, sourcePos, params.first(),
      // TODO: sourcePosForSubExpr
      buildDT(isPi, sourcePos, params.drop(1), body));
  }

  private static Expr buildLam(SourcePos sourcePos, SeqView<LocalVar> params, Expr body) {
    if (params.isEmpty()) return body;
    return new Expr.Lam(sourcePos, params.first(),
      // TODO: sourcePosForSubExpr
      buildLam(sourcePos, params.drop(1), body));
  }

  private @NotNull Param<Expr> param(Guest0x0Parser.ExprContext paramExpr) {
    return new Param<>(new LocalVar("_"), expr(paramExpr));
  }

  private SeqView<Param<Expr>> param(Guest0x0Parser.ParamContext param) {
    var e = expr(param.expr());
    return ImmutableSeq.from(param.ID()).view()
      .map(id -> new Param<>(new LocalVar(id.getText()), e));
  }

  // IN URGENT NEED OF AN ANTLR4 WRAPPER EXTRACTED FROM AYA

  private @NotNull SourcePos sourcePosOf(ParserRuleContext ctx) {
    var start = ctx.getStart();
    var end = ctx.getStop();
    return new SourcePos(
      source,
      start.getStartIndex(),
      end.getStopIndex(),
      start.getLine(),
      start.getCharPositionInLine(),
      end.getLine(),
      end.getCharPositionInLine() + end.getText().length() - 1
    );
  }
}
