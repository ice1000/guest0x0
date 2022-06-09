dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.jetbrains", "annotations", version = deps.getProperty("version.annotations"))
}

CommonTasks.fatJar(project, "org.aya.cube.compiler.CliMain")
