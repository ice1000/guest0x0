module org.aya.guest.cli {
  requires static org.jetbrains.annotations;
  requires org.antlr.antlr4.runtime;
  requires org.aya.guest.base;
  requires org.aya.guest.cubical;
  requires info.picocli;
  requires transitive kala.base;
  requires transitive kala.collection;
  requires transitive org.aya.pretty;
  requires transitive org.aya.repl;
  requires transitive org.aya.util;

  exports org.aya.guest0x0.cli;
}
