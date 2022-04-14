package org.aya.guest0x0.cli;

import kala.collection.SeqView;
import kala.collection.immutable.ImmutableSeq;
import org.antlr.v4.runtime.ParserRuleContext;
import org.aya.guest0x0.parser.Guest0x0Parser;
import org.aya.guest0x0.syntax.*;
import org.aya.repl.antlr.AntlrUtil;
import org.aya.util.error.SourceFile;
import org.aya.util.error.SourcePos;
import org.aya.util.error.WithPos;
import org.jetbrains.annotations.NotNull;

public record Parser(@NotNull SourceFile source) {
  public @NotNull Expr expr(@NotNull Guest0x0Parser.ExprContext expr) {
    return switch (expr) {
      case Guest0x0Parser.ParenContext paren -> expr(paren.expr());
      case Guest0x0Parser.TwoContext app -> new Expr.Two(true, sourcePosOf(app), expr(app.expr(0)), expr(app.expr(1)));
      case Guest0x0Parser.PairContext p -> new Expr.Two(false, sourcePosOf(p), expr(p.expr(0)), expr(p.expr(1)));
      case Guest0x0Parser.FstContext fst -> new Expr.Proj(sourcePosOf(fst), expr(fst.expr()), true);
      case Guest0x0Parser.SndContext snd -> new Expr.Proj(sourcePosOf(snd), expr(snd.expr()), false);
      case Guest0x0Parser.TreborContext trebor -> new Expr.UI(sourcePosOf(trebor), true);
      case Guest0x0Parser.IntervalContext interval -> new Expr.UI(sourcePosOf(interval), false);
      case Guest0x0Parser.LamContext lam -> buildLam(sourcePosOf(lam), ImmutableSeq.from(lam.ID()).view()
        .map(id -> new WithPos<>(AntlrUtil.sourcePosOf(id, source), new LocalVar(id.getText()))), expr(lam.expr()));
      case Guest0x0Parser.RefContext ref -> new Expr.Unresolved(sourcePosOf(ref), ref.ID().getText());
      case Guest0x0Parser.PiContext pi -> buildDT(true, sourcePosOf(pi), param(pi.param()), expr(pi.expr()));
      case Guest0x0Parser.SigContext si -> buildDT(false, sourcePosOf(si), param(si.param()), expr(si.expr()));
      case Guest0x0Parser.SimpFunContext pi -> new Expr.DT(true, sourcePosOf(pi), param(pi.expr(0)), expr(pi.expr(1)));
      case Guest0x0Parser.SimpTupContext si -> new Expr.DT(false, sourcePosOf(si), param(si.expr(0)), expr(si.expr(1)));
      case Guest0x0Parser.ILitContext il -> iPat(il.iPat());
      case Guest0x0Parser.TranspContext tp -> new Expr.Transp(sourcePosOf(tp), expr(tp.expr(0)), expr(tp.expr(1)));
      case Guest0x0Parser.InvContext in -> new Expr.Formula(sourcePosOf(in), new Formula.Inv<>(expr(in.expr())));
      case Guest0x0Parser.IConnContext ic -> new Expr.Formula(sourcePosOf(ic),
        new Formula.Conn<>(ic.AND() != null, expr(ic.expr(0)), expr(ic.expr(1))));
      case Guest0x0Parser.CubeContext cube -> new Expr.Path(sourcePosOf(cube), new Boundary.Data<>(
        ImmutableSeq.from(cube.ID()).map(id -> new LocalVar(id.getText())),
        expr(cube.expr()),
        ImmutableSeq.from(cube.boundary()).map(b -> new Boundary<>(face(b.face()), expr(b.expr())))));
      default -> throw new IllegalArgumentException("Unknown expr: " + expr.getClass().getName());
    };
  }

  private @NotNull Expr iPat(Guest0x0Parser.IPatContext iPat) {
    var pos = sourcePosOf(iPat);
    return iPat.LEFT() != null ? new Expr.Formula(pos, new Formula.Lit<>(true))
      : iPat.RIGHT() != null ? new Expr.Formula(pos, new Formula.Lit<>(false))
      : new Expr.Hole(pos, ImmutableSeq.empty());
  }

  private @NotNull Boundary.Face face(Guest0x0Parser.FaceContext face) {
    return new Boundary.Face(ImmutableSeq.from(face.iPat()).map(i ->
      i.LEFT() != null ? Boundary.Case.LEFT : i.RIGHT() != null
        ? Boundary.Case.RIGHT : Boundary.Case.VAR));
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

  private Expr buildDT(boolean isPi, SourcePos pos, SeqView<Param<Expr>> params, Expr body) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    return new Expr.DT(isPi, pos, params.first(), buildDT(isPi,
      AntlrUtil.sourcePosForSubExpr(source, drop.map(x -> x.type().pos()), body.pos()), drop, body));
  }

  private Expr buildLam(SourcePos pos, SeqView<WithPos<LocalVar>> params, Expr body) {
    if (params.isEmpty()) return body;
    var drop = params.drop(1);
    return new Expr.Lam(pos, params.first().data(), buildLam(
      AntlrUtil.sourcePosForSubExpr(source, drop.map(WithPos::sourcePos), body.pos()), drop, body));
  }

  private @NotNull Param<Expr> param(Guest0x0Parser.ExprContext paramExpr) {
    return new Param<>(new LocalVar("_"), expr(paramExpr));
  }

  private SeqView<Param<Expr>> param(Guest0x0Parser.ParamContext param) {
    var e = expr(param.expr());
    return ImmutableSeq.from(param.ID()).view()
      .map(id -> new Param<>(new LocalVar(id.getText()), e));
  }

  private @NotNull SourcePos sourcePosOf(ParserRuleContext ctx) {
    return AntlrUtil.sourcePosOf(ctx, source);
  }
}
