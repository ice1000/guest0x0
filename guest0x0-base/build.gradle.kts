// Copyright (c) 2020-2022 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
dependencies {
  val deps: java.util.Properties by rootProject.ext
  implementation("org.aya-prover", "tools", version = deps.getProperty("version.aya"))
  implementation("org.aya-prover", "pretty", version = deps.getProperty("version.aya"))
  implementation("org.antlr", "antlr4-runtime", version = deps.getProperty("version.antlr"))
  testImplementation("org.junit.jupiter", "junit-jupiter", version = deps.getProperty("version.junit"))
  testImplementation("org.hamcrest", "hamcrest", version = deps.getProperty("version.hamcrest"))
}

idea.module {
  sourceDirs.add(file("src/main/gen"))
}

tasks.named<Test>("test") {
  testLogging.showStandardStreams = true
  testLogging.showCauses = true
  inputs.dir(projectDir.resolve("src/test/resources"))
}
