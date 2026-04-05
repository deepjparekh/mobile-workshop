plugins {
  kotlin("jvm") version "2.0.21"
  application
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

kotlin { jvmToolchain(17) }

application { mainClass.set("dev.mobileworkshop.helper.MainKt") }

detekt {
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom("${rootDir}/config/detekt/detekt.yml")
}

tasks.named("check") { dependsOn("detekt") }
