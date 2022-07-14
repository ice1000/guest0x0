dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.aya-prover", "tools", version = deps.getProperty("version.aya"))
  api(project(":cubical"))
  testImplementation(project(":cli"))
  testImplementation("org.junit.jupiter", "junit-jupiter", version = deps.getProperty("version.junit"))
  testImplementation("org.hamcrest", "hamcrest", version = deps.getProperty("version.hamcrest"))
}

