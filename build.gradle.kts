// Copyright (c) 2020-2021 Yinsen (Tesla) Zhang.
// Use of this source code is governed by the GNU GPLv3 license that can be found in the LICENSE file.
import java.util.*

plugins {
  java
  idea
  `java-library`
  `maven-publish`
  signing
}

var deps: Properties by rootProject.ext

deps = Properties()
file("gradle/deps.properties").reader().use(deps::load)

allprojects {
  group = "org.aya-prover"
  version = deps.getProperty("version.project")
}

subprojects {
  apply {
    plugin("java")
    plugin("idea")
    plugin("maven-publish")
    plugin("java-library")
    plugin("signing")
  }

  java {
    withSourcesJar()
    if (hasProperty("release")) withJavadocJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
  }

  idea.module {
    outputDir = file("out/production")
    testOutputDir = file("out/test")
  }

  tasks.withType<JavaCompile>().configureEach {
    modularity.inferModulePath.set(true)

    options.apply {
      encoding = "UTF-8"
      isDeprecation = true
      release.set(17)
      compilerArgs.addAll(listOf("-Xlint:unchecked", "--enable-preview"))
    }
  }

  tasks.withType<Javadoc>().configureEach {
    val options = options as StandardJavadocDocletOptions
    options.addBooleanOption("-enable-preview", true)
    options.addStringOption("-source", "17")
    options.addStringOption("Xdoclint:none", "-quiet")
    options.encoding("UTF-8")
    options.tags(
      "apiNote:a:API Note:",
      "implSpec:a:Implementation Requirements:",
      "implNote:a:Implementation Note:",
    )
  }

  artifacts {
    add("archives", tasks["sourcesJar"])
    if (hasProperty("release")) add("archives", tasks["javadocJar"])
  }

  tasks.withType<Test>().configureEach {
    jvmArgs = listOf("--enable-preview")
    useJUnitPlatform()
    enableAssertions = true
    reports.junitXml.mergeReruns.set(true)
  }

  tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf("--enable-preview")
    enableAssertions = true
  }

  if (hasProperty("ossrhUsername")) publishing.repositories {
    maven("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2") {
      name = "MavenCentral"
      credentials {
        username = property("ossrhUsername").toString()
        password = property("ossrhPassword").toString()
      }
    }
  }

  val proj = this@subprojects
  publishing.publications {
    create<MavenPublication>("maven") {
      val githubUrl = "https://github.com/aya-prover/aya-dev"
      groupId = proj.group.toString()
      version = proj.version.toString()
      artifactId = proj.name
      from(components["java"])
      pom {
        description.set("Guest0x0 programming language")
        name.set(proj.name)
        url.set("https://www.aya-prover.org")
        licenses {
          license {
            name.set("GPL-3.0")
            url.set("$githubUrl/blob/master/LICENSE")
          }
        }
        developers {
          developer {
            id.set("ice1000")
            name.set("Tesla Zhang")
            email.set("ice1000kotlin@foxmail.com")
          }
        }
        scm {
          connection.set("scm:git:$githubUrl")
          url.set(githubUrl)
        }
      }
    }
  }

  if (hasProperty("signing.keyId")) signing {
    sign(publishing.publications["maven"])
  }
}
