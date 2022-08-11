module aya.guest.base {
  requires static org.jetbrains.annotations;

  requires aya.guest.cubical;

  requires transitive aya.pretty;
  requires transitive aya.util;
  requires transitive kala.base;
  requires transitive kala.collection;

  exports org.aya.guest0x0.syntax;
  exports org.aya.guest0x0.tyck;
  exports org.aya.guest0x0.util;
}
