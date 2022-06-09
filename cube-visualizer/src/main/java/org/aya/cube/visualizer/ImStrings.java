package org.aya.cube.visualizer;

import org.aya.cube.visualizer.CubeData.Side;
import org.ice1000.jimgui.JImStr;

import static java.util.Arrays.stream;

public interface ImStrings {
  JImStr[] tabItem = stream(Side.values())
    .map(s -> new JImStr(s.ordinal() + "##TabItem" + s.name())).toArray(JImStr[]::new);
  JImStr[] dashed = stream(Side.values())
    .map(s -> new JImStr("##Dashed" + s.name())).toArray(JImStr[]::new);
  JImStr[] equal = stream(Side.values())
    .map(s -> new JImStr("##Equal" + s.name())).toArray(JImStr[]::new);
  JImStr[] hidden = stream(Side.values())
    .map(s -> new JImStr("##Hidden" + s.name())).toArray(JImStr[]::new);


}
