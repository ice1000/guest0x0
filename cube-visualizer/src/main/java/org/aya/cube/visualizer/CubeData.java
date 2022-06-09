package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImStr;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

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
    return lines[orientation.ordinal()].isEqual().accessValue();
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
    public final JImStr equal;
    public final JImStr hidden;
    public final String from, to;

    Side(Orient adjacent0, Orient adjacent1) {
      this.adjacent0 = adjacent0;
      this.adjacent1 = adjacent1;
      var common = Arrays.stream(adjacent0.vertices).filter(adjacent1::contains).sorted().toArray();
      assert common.length == 2;
      from = Util.binPad3(common[0]);
      to = Util.binPad3(common[1]);
      tabItem = new JImStr(ordinal() + "##TabItem" + name());
      dashed = new JImStr("##Dash" + name());
      equal = new JImStr("##Equal" + name());
      hidden = new JImStr("##Hidden" + name());
    }
  }

  public enum Orient {
    Top(new int[]{0b000, 0b100, 0b101, 0b001}),
    Bottom(new int[]{0b010, 0b110, 0b111, 0b011}),
    Front(new int[]{0b001, 0b101, 0b111, 0b011}),
    Back(new int[]{0b000, 0b100, 0b110, 0b010}),
    Left(new int[]{0b000, 0b010, 0b011, 0b001}),
    Right(new int[]{0b100, 0b110, 0b111, 0b101});

    public final @NotNull JImStr[] toggle;
    public final @NotNull JImStr input;
    public final @NotNull JImStr tabItem;
    public final int @NotNull [] vertices;
    public final @NotNull String tikz;
    public final @NotNull String center;

    public boolean contains(int i) {
      return Arrays.stream(vertices).anyMatch(v -> v == i);
    }

    Orient(int @NotNull [] vertices) {
      this.vertices = vertices;
      tikz = Arrays.stream(vertices).mapToObj(i -> Util.apply3D(i, ($, x, y, z) -> x + "," + y + "," + z))
        .collect(Collectors.joining(") -- (", "(", ")"));
      var sums = new int[]{0, 0, 0};
      Arrays.stream(vertices).forEach(i -> Util.apply3D(i, ($, x, y, z) -> {
        sums[0] += x;
        sums[1] += y;
        return sums[2] += z;
      }));
      center = sums[0] / 4F + "," + sums[1] / 4F + "," + sums[2] / 4F;
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
          ") at (" + x + "," + y + "," + z + ") {\\(",
        isHighlight);
      builder.append(vertices[i].latex(), isHighlight);
      builder.appendln("\\)};", isHighlight);
      return null;
    });
    builder.appendln("\\end{pgfonlayer}", false);
    for (var orient : Orient.values()) {
      faces[orient.ordinal()].buildText(builder, orient, highlight == orient);
    }
    for (var side : Side.values()) {
      var isHighlight = highlight == side;
      var ptr = lines[side.ordinal()].serialize();
      if (ptr.isHidden()) continue;
      var attrs = new ArrayList<String>();
      if (ptr.isEqual()) attrs.add("equals arrow");
      if (ptr.isDashed()) attrs.add("dashed");
      builder.appendln("\\draw " + attrs +
        " (" + side.from +
        ") -- (" + side.to + ") ;", isHighlight);
    }
    builder.appendln("}", false);
  }
}
