module aya.guest.cli {
  requires static org.jetbrains.annotations;
  requires org.antlr.antlr4.runtime;
  requires aya.guest.base;
  requires aya.guest.cubical;
  requires info.picocli;
  requires transitive kala.base;
  requires transitive kala.collection;
  requires transitive org.aya.pretty;
  requires transitive org.aya.repl;
  requires transitive org.aya.util;

  exports org.aya.guest0x0.cli;
}
