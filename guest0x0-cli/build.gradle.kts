plugins { application }
application.mainClass.set("org.aya.guest0x0.cli.CliMain")

dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.antlr", "antlr4-runtime", version = deps.getProperty("version.antlr"))
  implementation(project(":guest0x0-base"))
  implementation("org.aya-prover", "tools-repl", version = deps.getProperty("version.aya"))
}

val genDir = "src/main/gen"
sourceSets["main"].java.srcDir(file(genDir))
idea.module {
  sourceDirs.add(file(genDir))
}
