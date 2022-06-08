package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.NativeBool;
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
  private final FaceData[] faces = new FaceData[Face3D.values().length];
  private @Nullable Face3D focusedFace;

  public enum Face3D {
    Top, Bottom, Front, Back, Left, Right;
  }

  public GuiMain(JImGui window) {
    this.window = window;
    cubeLen.modifyValue(50);
    for (var face : Face3D.values()) {
      //noinspection resource
      faces[face.ordinal()] = new FaceData();
    }
  }

  @Override public void close() {
    window.close();
    cubeLen.close();
    for (var face : faces) face.close();
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
      if (window.begin("Preview")) {
        previewWindow();
        window.end();
      }
      if (window.begin("Main Control")) {
        mainControlWindow();
        window.end();
      }
      window.render();
    }
  }

  private void mainControlWindow() {
    window.sliderFloat("Width", cubeLen, 5, 200);
    var hasHover = false;
    for (var face : Face3D.values()) {
      var ptr = faces[face.ordinal()];
      window.toggleButton("##Toggle" + face.name(), ptr.isEnabled());
      if (window.isItemHovered()) {
        hasHover = true;
        focusedFace = face;
      }
      if (ptr.enabled()) {
        window.sameLine();
        window.inputText("##Input" + face.name(), ptr.latex());
      }
      window.sameLine();
      window.text(face.name());
    }
    if (!hasHover) focusedFace = null;
  }

  private void previewWindow() {
    var x = window.getWindowPosX();
    var y = window.getWindowPosY();
    userLen = cubeLen.accessValue();
    projectedLen = userLen * 0.6F;
    var baseX = x + 30;
    var baseY = y + 50;
    if (faces[Face3D.Top.ordinal()].enabled()) hParallelogram(baseX, baseY); // Top
    if (faces[Face3D.Left.ordinal()].enabled()) vParallelogram(baseX, baseY); // Left
    if (faces[Face3D.Front.ordinal()].enabled()) square(baseX, baseY + projectedLen); // Front
    if (faces[Face3D.Bottom.ordinal()].enabled()) hParallelogram(baseX, baseY + userLen); // Bottom
    if (faces[Face3D.Back.ordinal()].enabled()) square(baseX + projectedLen, baseY); // Back
    if (faces[Face3D.Right.ordinal()].enabled()) vParallelogram(baseX + userLen, baseY); // Right
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
      0x88CCEE00 - alphaDiff);
  }
}
