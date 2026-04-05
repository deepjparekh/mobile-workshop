plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.serialization") version "2.0.21"
  application
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("com.gradleup.shadow") version "8.3.5"
}

kotlin { jvmToolchain(17) }

dependencies {
  implementation("com.github.ajalt.clikt:clikt:4.4.0")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}

application { mainClass.set("dev.mobileworkshop.helper.MainKt") }

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
  archiveBaseName.set("mobile-workshop-helper")
  archiveClassifier.set("all")
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom("${rootDir}/config/detekt/detekt.yml")
}

tasks.named("check") { dependsOn("detekt") }
