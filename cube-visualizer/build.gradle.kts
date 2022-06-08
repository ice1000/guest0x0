dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.jetbrains", "annotations", version = deps.getProperty("version.annotations"))
  api("org.glavo.kala", "kala-common", version = deps.getProperty("version.kala"))
  implementation("org.ice1000.jimgui", "core", version = deps.getProperty("version.jimgui"))
}
