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
    var userLen = cubeLen.accessValue();
    var originalX = x + 30;
    var originalY = y + 50;
    offsetX = originalX;
    offsetY = originalY;
    var projectedLen = userLen * 0.6F;
    hParallelogram(projectedLen, userLen); // Top
    vParallelogram(projectedLen, userLen); // Left
    offsetY += projectedLen;
    square(userLen); // Front
    offsetY = originalY + userLen;
    hParallelogram(projectedLen, userLen); // Bottom
    offsetY = originalY;
    offsetX += projectedLen;
    square(userLen); // Back
    offsetX = originalX + userLen;
    vParallelogram(projectedLen, userLen); // Right
  }

  private void hParallelogram(float height, float width) {
    var ui = window.getForegroundDrawList();
    ui.addQuadFilled(
      offsetX + height, offsetY,
      offsetX + height + width, offsetY,
      offsetX + width, offsetY + height,
      offsetX, offsetY + height,
      0x88AAFF00);
  }

  private void square(float width) {
    var ui = window.getForegroundDrawList();
    ui.addRectFilled(
      offsetX, offsetY,
      offsetX + width, offsetY + width,
      0x88BBEE11);
  }

  private void vParallelogram(float height, float width) {
    var ui = window.getForegroundDrawList();
    ui.addQuadFilled(
      offsetX + height, offsetY,
      offsetX + height, offsetY + width,
      offsetX, offsetY + width + height,
      offsetX, offsetY + height,
      0x88CCEE00);
  }
}
