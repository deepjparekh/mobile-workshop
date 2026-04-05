plugins {
  base
  id("com.diffplug.spotless") version "7.0.2"
}

group = "dev.mobileworkshop"

version = "0.1.0-SNAPSHOT"

allprojects {
  group = rootProject.group
  version = rootProject.version
}

spotless {
  kotlin {
    target("src/helper/kotlin/src/**/*.kt")
    ktfmt("0.53").googleStyle()
  }

  kotlinGradle {
    target("*.gradle.kts", "src/helper/kotlin/*.gradle.kts")
    ktfmt("0.53").googleStyle()
  }
}

tasks.register("quality") {
  group = LifecycleBasePlugin.VERIFICATION_GROUP
  description = "Runs Gradle-backed static analysis and formatting checks."
  dependsOn("spotlessCheck", ":helper:kotlin:detekt")
}

tasks.named("check") { dependsOn("quality") }
