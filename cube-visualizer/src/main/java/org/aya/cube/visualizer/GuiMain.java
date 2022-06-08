package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.NativeFloat;
import org.ice1000.jimgui.util.JniLoader;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("AccessStaticViaInstance")
public final class GuiMain implements AutoCloseable {
  private final JImGui window;
  private float offsetX;
  private float offsetY;
  private @NotNull NativeFloat width = new NativeFloat();
  private boolean[] faces = new boolean[Face3D.values().length];

  public enum Face3D {
    Top, Bottom, Front, Back, Left, Right;
  }

  public GuiMain(JImGui window) {this.window = window;}

  @Override public void close() {
    window.close();
  }

  public static void main(String... args) {
    JniLoader.load();
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
      window.render();
    }
  }

  private void previewWindow() {
    var x = window.getWindowPosX();
    var y = window.getWindowPosY();
    offsetX = x + 50;
    offsetY = y + 80;
    var userWidth = width.accessValue();
    hParallelogram(userWidth / 2, userWidth);
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
