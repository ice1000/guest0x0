module aya.guest.cli {
  requires static org.jetbrains.annotations;
  requires org.antlr.antlr4.runtime;
  requires aya.guest.base;
  requires aya.guest.cubical;
  requires info.picocli;
  requires transitive kala.base;
  requires transitive kala.collection;
  requires transitive aya.pretty;
  requires transitive aya.repl;
  requires transitive aya.util;

  exports org.aya.guest0x0.cli;
}
