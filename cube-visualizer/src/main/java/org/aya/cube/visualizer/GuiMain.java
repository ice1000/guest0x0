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
  private @Nullable CubeData.Orient focusedFace;
  private static final JImStr latexCodeStr = new JImStr("LaTeX code");

  public GuiMain(JImGui window) {
    this.window = window;
    cubeLen.modifyValue(50);
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
      window.render();
    }
  }

  private void mainControlWindow() {
    window.sliderFloat("Width", cubeLen, 5, 200);
    if (window.beginTabBar("Control")) {
      cubeFaces(cube);
      cubeEdges(cube);
      window.endTabBar();
    }
  }

  private void cubeEdges(CubeData cube) {
    if (!window.beginTabItem("Edges")) return;
    window.endTabItem();
  }

  private void cubeFaces(CubeData cube) {
    if (!window.beginTabItem("Faces")) return;
    var hasHover = false;
    for (var face : CubeData.Orient.values()) {
      var ptr = cube.faces()[face.ordinal()];
      window.toggleButton(face.toggle, ptr.isEnabled());
      if (window.isItemHovered()) {
        hasHover = true;
        focusedFace = face;
      }
      if (ptr.enabled()) {
        window.sameLine();
        window.inputTextWithHint(face.input,
          latexCodeStr, ptr.latex());
      }
      window.sameLine();
      window.text(face.name());
    }
    if (!hasHover) focusedFace = null;
    window.endTabItem();
  }

  private void previewWindow() {
    var x = window.getWindowPosX();
    var y = window.getWindowPosY();
    userLen = cubeLen.accessValue();
    projectedLen = userLen * 0.6F;
    drawCubeAt(x + 30, y + 50);
  }

  private void drawCubeAt(float baseX, float baseY) {
    if (wantDraw(CubeData.Orient.Top)) hParallelogram(baseX, baseY); // Top
    if (wantDraw(CubeData.Orient.Left)) vParallelogram(baseX, baseY); // Left
    if (wantDraw(CubeData.Orient.Front)) square(baseX, baseY + projectedLen); // Front
    if (wantDraw(CubeData.Orient.Bottom)) hParallelogram(baseX, baseY + userLen); // Bottom
    if (wantDraw(CubeData.Orient.Back)) square(baseX + projectedLen, baseY); // Back
    if (wantDraw(CubeData.Orient.Right)) vParallelogram(baseX + userLen, baseY); // Right
  }

  private boolean wantDraw(CubeData.Orient face) {
    if (focusedFace == face) {
      alphaDiff = 0x40000000;
      return true;
    } else alphaDiff = 0;
    return cube.enabled(face);
  }

  private void hParallelogram(float x, float y) {
    var ui = window.getForegroundDrawList();
    ui.addQuadFilled(
      x + projectedLen, y,
      x + projectedLen + userLen, y,
      x + userLen, y + projectedLen,
      x, y + projectedLen,
      0x88AAFF00 - alphaDiff);
  }

  private void square(float x, float y) {
    var ui = window.getForegroundDrawList();
    ui.addRectFilled(x, y, x + userLen, y + userLen,
      0x8811EEBB - alphaDiff);
  }

  private void vParallelogram(float x, float y) {
    var ui = window.getForegroundDrawList();
    ui.addQuadFilled(
      x + projectedLen, y,
      x + projectedLen, y + userLen,
      x, y + userLen + projectedLen,
      x, y + projectedLen,
      0x88CCAA33 - alphaDiff);
  }
}
