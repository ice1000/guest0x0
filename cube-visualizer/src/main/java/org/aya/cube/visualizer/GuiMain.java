package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.NativeBool;
import org.ice1000.jimgui.NativeFloat;
import org.ice1000.jimgui.util.JImGuiUtil;
import org.ice1000.jimgui.util.JniLoader;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("AccessStaticViaInstance")
public final class GuiMain implements AutoCloseable {
  private final JImGui window;
  private float offsetX;
  private float offsetY;
  private final @NotNull NativeFloat cubeLen = new NativeFloat();
  private NativeBool[] faces = new NativeBool[Face3D.values().length];

  public enum Face3D {
    Top, Bottom, Front, Back, Left, Right;
  }

  public GuiMain(JImGui window) {
    this.window = window;
    cubeLen.modifyValue(50);
    for (var face : Face3D.values()) {
      //noinspection resource
      faces[face.ordinal()] = new NativeBool();
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
    for (var face : Face3D.values()) {
      window.toggleButton(face.name(), faces[face.ordinal()]);
    }
  }

  private void previewWindow() {
    var x = window.getWindowPosX();
    var y = window.getWindowPosY();
    var userSize = cubeLen.accessValue();
    offsetX = x + 30;
    offsetY = y + 50;
    hParallelogram(userSize / 2, userSize);
    offsetY += userSize;
    hParallelogram(userSize / 2, userSize);
  }

  private void hParallelogram(float height, float width) {
    var ui = window.getForegroundDrawList();
    ui.addQuadFilled(
      offsetX + height, offsetY,
      offsetX + height + width, offsetY,
      offsetX + width, offsetY + height,
      offsetX, offsetY + height,
      0x88FFFF00);
  }

  private void vParallelogram(int height, int width) {
    var ui = window.getForegroundDrawList();
    ui.addQuadFilled(
      offsetX + height, offsetY,
      offsetX + height + width, offsetY,
      offsetX + width, offsetY + height,
      offsetX, offsetY + height,
      0x88FFFF00);
  }
}
