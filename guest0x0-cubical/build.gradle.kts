dependencies {
  val deps: java.util.Properties by rootProject.ext
  api("org.aya-prover", "pretty", version = deps.getProperty("version.aya"))
}
