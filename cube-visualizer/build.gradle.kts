dependencies {
  val deps: java.util.Properties by rootProject.ext
  // api("org.glavo.kala", "kala-common", version = deps.getProperty("version.kala"))
  api(project(":cube-compiler"))
  implementation("org.ice1000.jimgui", "core", version = deps.getProperty("version.jimgui"))
}
