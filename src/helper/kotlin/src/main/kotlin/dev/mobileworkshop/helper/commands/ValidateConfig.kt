package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.mobileworkshop.helper.models.WorkspaceDetection
import java.io.File
import kotlinx.serialization.json.Json

class ValidateConfig :
  CliktCommand(
    name = "validate-config",
    help = "Validates the .mobile-workshop/config.json file.",
  ) {
  private val repoPath by option("--repo", help = "Path to the repository").required()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val configFile = File(repoPath, ".mobile-workshop/config.json")
    if (!configFile.exists()) {
      echo("Error: Config file not found: ${configFile.absolutePath}", err = true)
      System.exit(1)
    }

    try {
      // In V1, config is just the confirmed workspace detection
      json.decodeFromString<WorkspaceDetection>(configFile.readText())
      println("Config is valid.")
    } catch (e: Exception) {
      echo("Error: Invalid config: ${e.message}", err = true)
      System.exit(1)
    }
  }
}
