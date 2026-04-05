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
    val metricsDir = File(repoPath, ".context/metrics")
    if (!metricsDir.exists()) metricsDir.mkdirs()

    // In V1, we just collect whatever metrics we can find.
    // For now, we'll assume we have some way to know agent metrics.
    // Actually, we'll just store the total for now.

    val metrics =
      RunMetrics(runId = runId, totalDurationMs = totalDurationMs, agentMetrics = emptyMap())

    val metricsFile = File(metricsDir, "run-metrics.json")
    metricsFile.writeText(json.encodeToString(metrics))

    println("Metrics for $runId collected.")
  }
}
