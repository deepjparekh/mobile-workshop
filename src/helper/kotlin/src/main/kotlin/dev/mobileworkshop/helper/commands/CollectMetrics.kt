package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RunMetrics(
  val runId: String,
  val totalDurationMs: Long,
  val agentMetrics: Map<String, AgentMetrics>,
)

@Serializable data class AgentMetrics(val durationMs: Long, val success: Boolean)

class CollectMetrics :
  CliktCommand(name = "collect-metrics", help = "Collects and aggregates run metrics.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val runId by option("--run-id", help = "Run ID").required()
  private val totalDurationMs by
    option("--duration", help = "Total duration in MS").long().required()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val stateDir = File(repoPath, ".context/state")
    val agentsDir = File(stateDir, "agents")
    val metricsDir = File(repoPath, ".context/metrics")
    if (!metricsDir.exists()) metricsDir.mkdirs()

    val agentMetrics = mutableMapOf<String, AgentMetrics>()

    if (agentsDir.exists()) {
      agentsDir
        .listFiles { _, name -> name.endsWith(".json") }
        ?.forEach { file ->
          // In V1, we just report success/failure from the agent state.
          // In a future version, we could measure duration more accurately.
          try {
            val state =
              Json.decodeFromString<dev.mobileworkshop.helper.models.AgentState>(file.readText())
            agentMetrics[state.agentId] =
              AgentMetrics(
                durationMs = 0, // Mock for now, V1 doesn't track agent-specific start/end times yet
                success = state.status == "completed",
              )
          } catch (e: Exception) {
            echo("Warning: Could not parse agent state for metrics: ${file.name}", err = true)
          }
        }
    }

    val metrics =
      RunMetrics(runId = runId, totalDurationMs = totalDurationMs, agentMetrics = agentMetrics)

    val metricsFile = File(metricsDir, "run-metrics.json")
    metricsFile.writeText(json.encodeToString(metrics))

    println("Metrics for $runId collected.")
  }
}
