rootProject.name = "guest0x0"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
  }
}

include(
  "guest0x0-base",
  "guest0x0-cli",
)
