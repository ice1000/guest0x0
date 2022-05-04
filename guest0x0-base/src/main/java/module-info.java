module org.aya.guest.base {
  requires static org.jetbrains.annotations;

  requires org.aya.guest.cubical;

  requires transitive org.aya.pretty;
  requires transitive org.aya.util;
  requires transitive kala.base;
  requires transitive kala.collection;

  exports org.aya.guest0x0.syntax;
  exports org.aya.guest0x0.tyck;
  exports org.aya.guest0x0.util;
}
