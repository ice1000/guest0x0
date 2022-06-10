package org.aya.cube.compiler;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public record CompiledLine(boolean isHidden, boolean isDashed, boolean isEqual) implements Serializable {
  public void buildText(@NotNull TextBuilder builder, Side side, boolean isHighlight) {
    if (isHidden) return;
    var fromTo = " (" + side.from + ") -- (" + side.to + ") ;";
    // builder.appendln("\\draw " + Arrays.asList("draw=white", "double", "very thick") + fromTo, isHighlight);
    var attrs = new ArrayList<String>();
    if (isEqual) attrs.add("equals arrow");
    if (isDashed) attrs.add("dashed");
    builder.appendln("\\draw " + attrs + fromTo, isHighlight);
  }

  public enum Side {
    TF(CompiledFace.Orient.Top, CompiledFace.Orient.Front), BF(CompiledFace.Orient.Bottom, CompiledFace.Orient.Front),
    TB(CompiledFace.Orient.Top, CompiledFace.Orient.Back), BB(CompiledFace.Orient.Bottom, CompiledFace.Orient.Back),
    TL(CompiledFace.Orient.Top, CompiledFace.Orient.Left), BL(CompiledFace.Orient.Bottom, CompiledFace.Orient.Left),
    TR(CompiledFace.Orient.Top, CompiledFace.Orient.Right), BR(CompiledFace.Orient.Bottom, CompiledFace.Orient.Right),
    LF(CompiledFace.Orient.Left, CompiledFace.Orient.Front),
    RF(CompiledFace.Orient.Right, CompiledFace.Orient.Front),
    LB(CompiledFace.Orient.Left, CompiledFace.Orient.Back),
    RB(CompiledFace.Orient.Right, CompiledFace.Orient.Back);

    public final CompiledFace.Orient adjacent0;
    public final CompiledFace.Orient adjacent1;
    public final String from, to;

    Side(CompiledFace.Orient adjacent0, CompiledFace.Orient adjacent1) {
      this.adjacent0 = adjacent0;
      this.adjacent1 = adjacent1;
      var common = Arrays.stream(adjacent0.vertices).filter(adjacent1::contains).sorted().toArray();
      assert common.length == 2;
      from = Util.binPad3(common[0]);
      to = Util.binPad3(common[1]);
    }
  }
}
