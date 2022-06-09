package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImStr;
import org.ice1000.jimgui.NativeFloat;
import org.ice1000.jimgui.util.JImGuiUtil;
import org.ice1000.jimgui.util.JniLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("AccessStaticViaInstance")
public final class GuiMain implements AutoCloseable {
  private final JImGui window;
  private final @NotNull NativeFloat cubeLen = new NativeFloat();
  private float userLen;
  private float projectedLen;
  private int alphaDiff = 0;
  private final CubeData cube = new CubeData();
  /** {@link CubeData.Orient} or {@link CubeData.Side} */
  private @Nullable Object highlight;
  private static final JImStr latexCodeStr = new JImStr("LaTeX code");
  private float thickness = 3F;

  public GuiMain(JImGui window) {
    this.window = window;
    cubeLen.modifyValue(100);
  }

  @Override public void close() {
    window.close();
    cubeLen.close();
    cube.close();
  }

  public static void main(String... args) {
    JniLoader.load();
    JImGuiUtil.cacheStringToBytes();
    try (var ui = new GuiMain(new JImGui("Cube visualizer"))) {
      ui.mainLoop();
    }
  }

  public void mainLoop() {
    while (!window.windowShouldClose()) {
      window.initNewFrame();
      if (window.begin("Rumor")) {
        previewWindow();
        window.end();
      }
      if (window.begin("Ground Control")) {
        mainControlWindow();
        window.end();
      }
      if (window.begin("Early Song")) {
        toStringWindow();
        window.end();
      }
      window.render();
    }
  }

  private void toStringWindow() {
    if (window.button("Copy")) {
      var sb = new TextBuilder.Strings();
      cube.buildText(sb, highlight);
      window.setClipboardText(sb.sb().toString());
    }
    window.sameLine();
    if (window.button("Copy preamble")) {
      window.setClipboardText(Util.carloPreamble);
    }
    cube.buildText(new TextBuilder.ImGui(window), highlight);
  }

  private void mainControlWindow() {
    window.text("FPS: " + window.getIO().getFramerate());
    window.sliderFloat("Width", cubeLen, 5, 200);
    var hasHover = cubeFaces(cube);
    window.separator();
    hasHover = cubeEdges(cube) || hasHover;
    window.separator();
    hasHover = cubePoints(cube) || hasHover;
    if (!hasHover) highlight = null;
  }

  private boolean cubePoints(CubeData cube) {
    window.text("Points");
    if (!window.beginTabBar("Points")) return false;
    var hasHover = false;
    for (var i = 0; i <= 0b111; ++i) {
      var name = Util.binPad3(i);
      var beginTabItem = window.beginTabItem(name + "##Pt");
      if (window.isItemHovered()) {
        hasHover = true;
        highlight = i;
      }
      if (!beginTabItem) continue;
      var ptr = cube.vertices()[i];
      window.inputText("##Input" + name, ptr.latex());
      window.endTabItem();
    }
    window.endTabBar();
    return hasHover;
  }

  private boolean cubeEdges(CubeData cube) {
    window.text("Lines");
    if (!window.beginTabBar("Edges")) return false;
    var hasHover = false;
    for (var side : CubeData.Side.values()) {
      var beginTabItem = window.beginTabItem(side.tabItem);
      if (window.isItemHovered()) {
        hasHover = true;
        highlight = side;
      }
      if (!beginTabItem) continue;
      var ptr = cube.lines()[side.ordinal()];
      window.toggleButton(side.hidden, ptr.isHidden());
      window.sameLine();
      window.text("Hidden");
      window.toggleButton(side.dashed, ptr.isDashed());
      window.sameLine();
      window.text("Dashed");
      window.toggleButton(side.equal, ptr.isEqual());
      window.sameLine();
      window.text("Equal");
      window.endTabItem();
    }
    window.endTabBar();
    return hasHover;
  }

  private boolean cubeFaces(CubeData cube) {
    if (!window.beginTabBar("Faces")) return false;
    var hasHover = false;
    for (var face : CubeData.Orient.values()) {
      var beginTabItem = window.beginTabItem(face.tabItem);
      if (window.isItemHovered()) {
        hasHover = true;
        highlight = face;
      }
      if (!beginTabItem) continue;
      var ptr = cube.faces()[face.ordinal()];
      for (var status : FaceData.Status.values()) {
        window.radioButton(face.toggle[status.ordinal()], ptr.status(), status.ordinal());
        if (status == FaceData.Status.Lines && window.isItemHovered()) {
          window.beginTooltip();
          window.text("Displayed as shaded");
          window.endTooltip();
        }
      }
      window.inputTextWithHint(face.input, latexCodeStr, ptr.latex());
      window.endTabItem();
    }
    window.endTabBar();
    return hasHover;
  }

  private void previewWindow() {
    var x = window.getWindowPosX();
    var y = window.getWindowPosY();
    userLen = cubeLen.accessValue();
    projectedLen = userLen * 0.6F;
    drawCubeAt(x + 30, y + 30);
  }

  private void drawCubeAt(float baseX, float baseY) {
    if (wantDraw(CubeData.Orient.Top)) hParallelogram(baseX, baseY); // Top
    if (wantDraw(CubeData.Orient.Left)) vParallelogram(baseX, baseY); // Left
    if (wantDraw(CubeData.Orient.Front)) square(baseX, baseY + projectedLen); // Front
    if (wantDraw(CubeData.Orient.Bottom)) hParallelogram(baseX, baseY + userLen); // Bottom
    if (wantDraw(CubeData.Orient.Back)) square(baseX + projectedLen, baseY); // Back
    if (wantDraw(CubeData.Orient.Right)) vParallelogram(baseX + userLen, baseY); // Right

    // Top
    if (wantDraw(CubeData.Side.TF)) hline(baseX, baseY + projectedLen);
    if (wantDraw(CubeData.Side.TB)) hline(baseX + projectedLen, baseY);
    if (wantDraw(CubeData.Side.TL)) aline(baseX, baseY);
    if (wantDraw(CubeData.Side.TR)) aline(baseX + userLen, baseY);

    // Bottom
    if (wantDraw(CubeData.Side.BF)) hline(baseX, baseY + projectedLen + userLen);
    if (wantDraw(CubeData.Side.BB)) hline(baseX + projectedLen, baseY + userLen);
    if (wantDraw(CubeData.Side.BL)) aline(baseX, baseY + userLen);
    if (wantDraw(CubeData.Side.BR)) aline(baseX + userLen, baseY + userLen);

    // Side edges
    if (wantDraw(CubeData.Side.LB)) vline(baseX + projectedLen, baseY);
    if (wantDraw(CubeData.Side.LF)) vline(baseX, baseY + projectedLen);
    if (wantDraw(CubeData.Side.RB)) vline(baseX + projectedLen + userLen, baseY);
    if (wantDraw(CubeData.Side.RF)) vline(baseX + userLen, baseY + projectedLen);

    var ui = window.getWindowDrawList();
    Util.forEach3D((i, x, y, z) -> {
      var centreX = baseX + projectedLen + x * userLen - z * projectedLen;
      var centreY = baseY + y * userLen + z * projectedLen;
      if (highlight == Integer.valueOf(i)) {
        ui.addCircleFilled(centreX, centreY, 4F, 0xFF0000FF);
      } else ui.addCircle(centreX, centreY, 4F, 0xFF0000FF);
      return null;
    });
  }

  private boolean wantDraw(CubeData.Orient face) {
    if (highlight == face) {
      alphaDiff = 0x40000000;
      return true;
    } else alphaDiff = 0;
    return cube.enabled(face);
  }

  private boolean wantDraw(CubeData.Side side) {
    thickness = cube.doubled(side) ? 3F : 1F;
    if (highlight == side) {
      alphaDiff = 0x40000000;
      return true;
    } else alphaDiff = 0;
    return cube.enabled(side);
  }

  private void hline(float x, float y) {
    var ui = window.getWindowDrawList();
    ui.addLine(x, y, x + userLen, y, 0x99DDA0DD + alphaDiff, thickness);
  }

  private void vline(float x, float y) {
    var ui = window.getWindowDrawList();
    ui.addLine(x, y, x, y + userLen, 0x99DDA0DD + alphaDiff, thickness);
  }

  private void aline(float x, float y) {
    var ui = window.getWindowDrawList();
    ui.addLine(x + projectedLen, y, x, y + projectedLen, 0x99DDA0DD + alphaDiff, thickness);
  }

  private void hParallelogram(float x, float y) {
    var ui = window.getWindowDrawList();
    ui.addQuadFilled(
      x + projectedLen, y,
      x + projectedLen + userLen, y,
      x + userLen, y + projectedLen,
      x, y + projectedLen,
      0x88AAFF00 - alphaDiff);
  }

  private void square(float x, float y) {
    var ui = window.getWindowDrawList();
    ui.addRectFilled(x, y, x + userLen, y + userLen,
      0x8811EEBB - alphaDiff);
  }

  private void vParallelogram(float x, float y) {
    var ui = window.getWindowDrawList();
    ui.addQuadFilled(
      x + projectedLen, y,
      x + projectedLen, y + userLen,
      x, y + userLen + projectedLen,
      x, y + projectedLen,
      0x88CCAA33 - alphaDiff);
  }
}
