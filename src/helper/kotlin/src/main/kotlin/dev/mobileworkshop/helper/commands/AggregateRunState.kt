package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.mobileworkshop.helper.models.AgentState
import dev.mobileworkshop.helper.models.RunState
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AggregateRunState :
  CliktCommand(
    name = "aggregate-run-state",
    help = "Aggregates agent states into a single run state.",
  ) {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val runId by option("--run-id", help = "Run ID").required()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val stateDir = File(repoPath, ".context/state")
    val agentsDir = File(stateDir, "agents")
    val runFile = File(stateDir, "run.json")

    val agentStates = mutableMapOf<String, AgentState>()
    if (agentsDir.exists()) {
      agentsDir
        .listFiles { _, name -> name.endsWith(".json") }
        ?.forEach { file ->
          try {
            val state = json.decodeFromString<AgentState>(file.readText())
            agentStates[state.agentId] = state
          } catch (e: SerializationException) {
            echo("Warning: Could not read agent state from ${file.name}: ${e.message}", err = true)
          } catch (e: IOException) {
            echo("Warning: Could not read agent state from ${file.name}: ${e.message}", err = true)
          }
        }
    }

    val existingRun =
      if (runFile.exists()) {
        try {
          json.decodeFromString<RunState>(runFile.readText())
        } catch (e: SerializationException) {
          echo("Warning: Could not read existing run state: ${e.message}", err = true)
          null
        } catch (e: IOException) {
          echo("Warning: Could not read existing run state: ${e.message}", err = true)
          null
        }
      } else null

    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
    val runState =
      RunState(
        runId = runId,
        status = determineRunStatus(agentStates.values),
        startedAt = existingRun?.startedAt ?: now,
        updatedAt = now,
        agents = agentStates,
      )

    val tempFile = File.createTempFile("run", ".json", stateDir)
    tempFile.writeText(json.encodeToString(runState))
    Files.move(
      tempFile.toPath(),
      runFile.toPath(),
      StandardCopyOption.ATOMIC_MOVE,
      StandardCopyOption.REPLACE_EXISTING,
    )

    println("Run state for $runId aggregated.")
  }

  private fun determineRunStatus(agents: Collection<AgentState>): String {
    return when {
      agents.isEmpty() || agents.all { it.status == "idle" } -> "initialized"
      agents.any { it.status == "failed" } -> "partially_failed"
      agents.any { it.status == "running" } -> "running"
      agents.all { it.status == "completed" } -> "completed"
      else -> "running"
    }
  }
}
