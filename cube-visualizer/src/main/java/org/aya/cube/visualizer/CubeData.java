package org.aya.cube.visualizer;

import org.aya.cube.compiler.CompiledCube;
import org.aya.cube.compiler.CompiledFace;
import org.aya.cube.compiler.CompiledLine;
import org.aya.cube.compiler.CompiledPoint;
import org.ice1000.jimgui.NativeString;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record CubeData(
  @NotNull NativeString name,
  @NotNull PointData @NotNull [] vertices,
  @NotNull LineData @NotNull [] lines,
  @NotNull FaceData @NotNull [] faces
) implements AutoCloseable {
  public CubeData() {
    this(
      new NativeString(),
      new PointData[0b1000],
      new LineData[CompiledLine.Side.values().length],
      new FaceData[CompiledFace.Orient.values().length]);
  }

  @SuppressWarnings("resource") public CubeData {
    for (var face : CompiledFace.Orient.values()) faces[face.ordinal()] = new FaceData();
    for (var side : CompiledLine.Side.values()) lines[side.ordinal()] = new LineData();
    for (int i = 0; i < vertices.length; i++) vertices[i] = new PointData();
  }

  public boolean enabled(CompiledFace.Orient orientation) {
    return faces[orientation.ordinal()].enabled();
  }

  public boolean enabled(CompiledLine.Side orientation) {
    return !lines[orientation.ordinal()].isHidden().accessValue();
  }

  public boolean doubled(CompiledLine.Side orientation) {
    return lines[orientation.ordinal()].isEqual().accessValue();
  }

  @Override public void close() {
    for (var v : faces) v.close();
    for (var v : lines) v.close();
    for (var v : vertices) v.close();
    name.close();
  }

  public @NotNull CompiledCube serialize() {
    return new CompiledCube(
      name.toBytes(),
      Arrays.stream(vertices).map(PointData::serialize).toArray(CompiledPoint[]::new),
      Arrays.stream(lines).map(LineData::serialize).toArray(CompiledLine[]::new),
      Arrays.stream(faces).map(FaceData::serialize).toArray(CompiledFace[]::new));
  }

  public void deserialize(@NotNull CompiledCube serialized) {
    for (var i = 0; i < vertices.length; i++) vertices[i].deserialize(serialized.vertices()[i]);
    for (var i = 0; i < lines.length; i++) lines[i].deserialize(serialized.lines()[i]);
    for (var i = 0; i < faces.length; i++) faces[i].deserialize(serialized.faces()[i]);
  }
}
