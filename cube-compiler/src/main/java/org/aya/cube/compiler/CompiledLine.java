package org.aya.cube.compiler;

import org.aya.cube.compiler.CompiledFace.Orient;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public record CompiledLine(
  byte @NotNull [] code,
  boolean isHidden,
  boolean isDashed,
  boolean isEqual
) implements Serializable {
  public CompiledLine {
    if (code == null) code = "".getBytes(StandardCharsets.US_ASCII);
  }

  public void buildText(@NotNull TextBuilder builder, Side side, boolean isHighlight) {
    if (isHidden) return;
    var fromTo = "(" + side.from + ")--(" + side.to + ")";
    builder.appendln("\\draw[line width=2pt,draw=white,draw opacity=0.7]" + fromTo + ";", isHighlight);
    var attrs = new ArrayList<String>();
    attrs.add(isEqual ? "equals arrow" : "->");
    if (isDashed) attrs.add("dashed");
    builder.append("\\draw" + attrs + fromTo + "node" + Arrays.asList("midway", "above", "sloped"), isHighlight);
    builder.append("{", isHighlight);
    if (code.length > 0) {
      builder.append(code, isHighlight);
    }
    builder.appendln("};", isHighlight);
  }

  public enum Side {
    TF(Orient.Top, Orient.Front), BF(Orient.Bottom, Orient.Front),
    TB(Orient.Top, Orient.Back), BB(Orient.Bottom, Orient.Back),
    TL(Orient.Top, Orient.Left), BL(Orient.Bottom, Orient.Left),
    TR(Orient.Top, Orient.Right), BR(Orient.Bottom, Orient.Right),
    LF(Orient.Left, Orient.Front),
    RF(Orient.Right, Orient.Front),
    LB(Orient.Left, Orient.Back),
    RB(Orient.Right, Orient.Back);

    public final Orient adjacent0;
    public final Orient adjacent1;
    public final String from, to;

    Side(Orient adjacent0, Orient adjacent1) {
      this.adjacent0 = adjacent0;
      this.adjacent1 = adjacent1;
      var common = Arrays.stream(adjacent0.vertices).filter(adjacent1::contains).sorted().toArray();
      assert common.length == 2;
      from = Util.binPad3(common[0]);
      to = Util.binPad3(common[1]);
    }

    boolean isLowerLayer() {
      return Arrays.asList(from, to).contains("010");
    }
  }
}
