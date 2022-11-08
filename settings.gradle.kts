rootProject.name = "guest0x0"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
  }
}

include(
  "cubical",
  // "base",
  // "cli",
  "cube-visualizer",
  "cube-compiler",
)
