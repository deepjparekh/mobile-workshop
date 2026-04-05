package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.mobileworkshop.helper.models.WorkspaceDetection
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConfirmWorkspace :
  CliktCommand(name = "confirm-workspace", help = "Confirms and persists the workspace map.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val detectionJson by
    option("--detection-json", help = "The JSON output from discover-workspace").required()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val detection =
      try {
        json.decodeFromString<WorkspaceDetection>(detectionJson)
      } catch (e: SerializationException) {
        echo("Error: Invalid detection JSON: ${e.message}", err = true)
        System.exit(1)
      }

    val dotMobileWorkshop = File(repoPath, ".mobile-workshop")
    if (!dotMobileWorkshop.exists()) dotMobileWorkshop.mkdirs()

    val configFile = File(dotMobileWorkshop, "config.json")

    try {
      // Simple config format for V1: just the modules
      configFile.writeText(json.encodeToString(detection))
      println("Workspace map confirmed and persisted to ${configFile.absolutePath}")
    } catch (e: IOException) {
      echo("Error: Could not write config file: ${e.message}", err = true)
      System.exit(1)
    }
  }
}
