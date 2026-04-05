package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.mobileworkshop.helper.models.AgentState
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UpdateAgentState :
  CliktCommand(name = "update-agent-state", help = "Updates the state of a single agent.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val agentId by option("--agent", help = "Agent ID").required()
  private val status by option("--status", help = "Agent status").required()
  private val currentTask by option("--task", help = "Current task")

  private val json = Json { prettyPrint = true }

  override fun run() {
    val agentsDir = File(repoPath, ".context/state/agents")
    if (!agentsDir.exists()) agentsDir.mkdirs()

    val state =
      AgentState(
        agentId = agentId,
        status = status,
        lastMessageAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString(),
        currentTask = currentTask,
      )

    val stateFile = File(agentsDir, "$agentId.json")
    val tempFile = File.createTempFile(agentId, ".json", agentsDir)
    tempFile.writeText(json.encodeToString(state))
    Files.move(
      tempFile.toPath(),
      stateFile.toPath(),
      StandardCopyOption.ATOMIC_MOVE,
      StandardCopyOption.REPLACE_EXISTING,
    )

    println("Agent state for $agentId updated to $status.")
  }
}
