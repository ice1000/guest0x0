dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.aya-prover", "pretty", version = deps.getProperty("version.aya"))
  testImplementation(project(":cli"))
  testImplementation("org.junit.jupiter", "junit-jupiter", version = deps.getProperty("version.junit"))
  testImplementation("org.hamcrest", "hamcrest", version = deps.getProperty("version.hamcrest"))
}
