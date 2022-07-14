module aya.guest.cubical {
  requires static org.jetbrains.annotations;

  requires transitive org.aya.pretty;
  requires transitive kala.base;
  requires transitive kala.collection;

  exports org.aya.guest0x0.cubical;
}
