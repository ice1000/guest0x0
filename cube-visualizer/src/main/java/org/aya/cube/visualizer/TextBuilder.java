package org.aya.cube.visualizer;

import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImVec4;
import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

public interface TextBuilder {
  void append(@NotNull NativeString text, boolean highlight);
  void append(@NotNull String text, boolean highlight);
  default void appendln(@NotNull String text, boolean highlight) {
    append(text, highlight);
    append("\n", false);
  }

  record ImGui(@NotNull JImGui ui) implements TextBuilder {
    @Override public void append(@NotNull NativeString text, boolean highlight) {
      if (highlight) ui.textColored(JImVec4.fromU32(0xFF00FF00), text.toString());
      else ui.text(text);
      ui.sameLine();
    }

    @Override public void append(@NotNull String text, boolean highlight) {
      appendln(text, highlight);
      ui.sameLine();
    }

    @Override public void appendln(@NotNull String text, boolean highlight) {
      if (highlight) ui.textColored(JImVec4.fromU32(0xFF00FF00), text);
      else ui.text(text);
    }
  }

  record Strings(@NotNull StringBuilder sb) implements TextBuilder {
    public Strings() {this(new StringBuilder());}

    @Override public void append(@NotNull NativeString text, boolean highlight) {
      append(text.toString(), highlight);
    }

    @Override public void append(@NotNull String text, boolean highlight) {
      sb.append(text);
    }

    @Override public String toString() {
      return sb.toString();
    }
  }
}
