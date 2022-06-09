package org.aya.cube.visualizer;

import org.aya.cube.compiler.CompiledFace;
import org.aya.cube.compiler.CompiledLine;
import org.ice1000.jimgui.JImStr;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public interface ImData {
  JImStr[] sideTabItem = Arrays.stream(CompiledLine.Side.values())
    .map(s -> new JImStr(s.ordinal() + "##TabItem" + s.name()))
    .toArray(JImStr[]::new);
  JImStr[] sideDashed = Arrays.stream(CompiledLine.Side.values())
    .map(s -> new JImStr("##Dashed" + s.name()))
    .toArray(JImStr[]::new);
  JImStr[] sideEqual = Arrays.stream(CompiledLine.Side.values())
    .map(s -> new JImStr("##Equal" + s.name()))
    .toArray(JImStr[]::new);
  JImStr[] sideHidden = Arrays.stream(CompiledLine.Side.values())
    .map(s -> new JImStr("##Hidden" + s.name()))
    .toArray(JImStr[]::new);


  @NotNull JImStr[][] orientToggle = Arrays.stream(CompiledFace.Orient.values())
    .map(o -> Arrays.stream(CompiledFace.Status.values())
      .map(s -> new JImStr(s.name() + "##Toggle" + o.name() + s.name()))
      .toArray(JImStr[]::new))
    .toArray(JImStr[][]::new);
  @NotNull JImStr[] orientInput = Arrays.stream(CompiledFace.Orient.values())
    .map(o -> new JImStr("##Input" + o.name()))
    .toArray(JImStr[]::new);
  @NotNull JImStr[] orientTabItem = Arrays.stream(CompiledFace.Orient.values())
    .map(o -> new JImStr(o.name() + "##TabItem" + o.name()))
    .toArray(JImStr[]::new);

  JImStr latexCodeStr = new JImStr("LaTeX code");

  enum ID {
    CubeRadio(1),
    StatusRadio(2);
    public final int id;

    ID(int id) {this.id = id;}
  }
}
