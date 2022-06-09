package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImStr;
import org.jetbrains.annotations.NotNull;

public record CubeData(
  @NotNull PointData @NotNull [] vertices,
  @NotNull LineData @NotNull [] lines,
  @NotNull FaceData @NotNull [] faces
) implements AutoCloseable {
  public CubeData() {
    this(
      new PointData[0b1000],
      new LineData[Side.values().length],
      new FaceData[Orient.values().length]);
  }

  @SuppressWarnings("resource") public CubeData {
    for (var face : Orient.values()) faces[face.ordinal()] = new FaceData();
    for (var side : Side.values()) lines[side.ordinal()] = new LineData();
    for (int i = 0; i < vertices.length; i++) vertices[i] = new PointData();
  }

  public boolean enabled(Orient orientation) {
    return faces[orientation.ordinal()].enabled();
  }

  public boolean enabled(Side orientation) {
    return !lines[orientation.ordinal()].isHidden().accessValue();
  }

  public boolean doubled(Side orientation) {
    return lines[orientation.ordinal()].isDoubled().accessValue();
  }

  @Override public void close() {
    for (var v : faces) v.close();
    for (var v : lines) v.close();
    for (var v : vertices) v.close();
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
    public final JImStr tabItem;
    public final JImStr dashed;
    public final JImStr doubled;
    public final JImStr hidden;

    Side(Orient adjacent0, Orient adjacent1) {
      this.adjacent0 = adjacent0;
      this.adjacent1 = adjacent1;
      tabItem = new JImStr(ordinal() + "##TabItem" + name());
      dashed = new JImStr("##Dash" + name());
      doubled = new JImStr("##DL" + name());
      hidden = new JImStr("##Hidden" + name());
    }
  }

  public enum Orient {
    Top("(0,0,0) -- (1,0,0) -- (1,0,1) -- (0,0,1)"),
    Bottom("(0,1,0) -- (1,1,0) -- (1,1,1) -- (0,1,1)"),
    Front("(0,0,1) -- (1,0,1) -- (1,1,1) -- (0,1,1)"),
    Back("(0,0,0) -- (1,0,0) -- (1,1,0) -- (0,1,0)"),
    Left("(0,0,0) -- (0,1,0) -- (0,1,1) -- (0,0,1)"),
    Right("(1,0,0) -- (1,1,0) -- (1,1,1) -- (1,0,1)");

    public final @NotNull JImStr[] toggle;
    public final @NotNull JImStr input;
    public final @NotNull JImStr tabItem;
    public final @NotNull String tikz;

    Orient(@NotNull String tikz) {
      this.tikz = tikz;
      input = new JImStr("##Input" + name());
      toggle = new JImStr[FaceData.Status.values().length];
      for (var status : FaceData.Status.values())
        toggle[status.ordinal()] = new JImStr(status.name() + "##Toggle" + name() + status.name());
      tabItem = new JImStr(name() + "##TabItem" + name());
    }
  }

  public void buildText(@NotNull TextBuilder builder, Object highlight) {
    builder.appendln("\\carloTikZ{\\begin{pgfonlayer}{frontmost}", false);
    Util.forEach3D((i, x, y, z) -> {
      var isHighlight = highlight == Integer.valueOf(i);
      builder.append("\\node (" + Util.binPad3(i) +
          ") at (" + x + " , " + y + " , " + z + ") {\\(",
        isHighlight);
      builder.append(vertices[i].latex(), isHighlight);
      builder.appendln("\\)};", isHighlight);
    });
    builder.appendln("\\end{pgfonlayer}", false);
    for (var orient : Orient.values()) {
      var isHighlight = highlight == orient;
      var ptr = faces[orient.ordinal()];
      var status = ptr.status().accessValue();
      if (status == FaceData.Status.Shaded.ordinal()) {
        builder.appendln("\\draw [draw=white,line width=3pt,fill=black!50,fill opacity=0.5]", isHighlight);
      } else if (status == FaceData.Status.Lines.ordinal()) {
        builder.appendln("\\fill [pattern color=gray,pattern=north west lines]", isHighlight);
      } else continue;
      builder.appendln(orient.tikz + " -- cycle ;", isHighlight);
    }
    builder.appendln("}", false);
  }
}
