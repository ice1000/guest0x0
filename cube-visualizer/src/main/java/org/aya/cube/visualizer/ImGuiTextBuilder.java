package org.aya.cube.visualizer;

import org.aya.cube.compiler.TextBuilder;
import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImVec4;
import org.jetbrains.annotations.NotNull;

public record ImGuiTextBuilder(@NotNull JImGui ui) implements TextBuilder {
  public static final @NotNull JImVec4 greenWheel = JImVec4.fromU32(0xFF00FF00);

  @Override public void append(@NotNull String text, boolean highlight) {
    appendln(text, highlight);
    ui.sameLine();
  }

  @Override public void appendln(@NotNull String text, boolean highlight) {
    if (highlight) ui.textColored(greenWheel, text);
    else ui.text(text);
  }
}
