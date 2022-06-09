package org.aya.cube.visualizer;

import org.aya.cube.visualizer.CubeData.Orient;
import org.aya.cube.visualizer.CubeData.Side;
import org.ice1000.jimgui.JImStr;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public interface ImStrings {
  JImStr[] sideTabItem = Arrays.stream(Side.values())
    .map(s -> new JImStr(s.ordinal() + "##TabItem" + s.name()))
    .toArray(JImStr[]::new);
  JImStr[] sideDashed = Arrays.stream(Side.values())
    .map(s -> new JImStr("##Dashed" + s.name()))
    .toArray(JImStr[]::new);
  JImStr[] sideEqual = Arrays.stream(Side.values())
    .map(s -> new JImStr("##Equal" + s.name()))
    .toArray(JImStr[]::new);
  JImStr[] sideHidden = Arrays.stream(Side.values())
    .map(s -> new JImStr("##Hidden" + s.name()))
    .toArray(JImStr[]::new);


  @NotNull JImStr[][] orientToggle = Arrays.stream(Orient.values())
    .map(o -> Arrays.stream(FaceData.Status.values())
      .map(s -> new JImStr(s.name() + "##Toggle" + o.name() + s.name()))
      .toArray(JImStr[]::new))
    .toArray(JImStr[][]::new);
  @NotNull JImStr[] orientInput = Arrays.stream(Orient.values())
    .map(o -> new JImStr("##Input" + o.name()))
    .toArray(JImStr[]::new);
  @NotNull JImStr[] orientTabItem = Arrays.stream(Orient.values())
    .map(o -> new JImStr(o.name() + "##TabItem" + o.name()))
    .toArray(JImStr[]::new);
}
