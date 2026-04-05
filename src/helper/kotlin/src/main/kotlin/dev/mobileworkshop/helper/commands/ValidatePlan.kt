package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.mobileworkshop.helper.models.ExecutionGraph
import java.io.File
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class ValidatePlan :
  CliktCommand(
    name = "validate-plan",
    help = "Validates an execution plan (execution-graph.json).",
  ) {
  private val planPath by option("--plan", help = "Path to the execution plan").required()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val planFile = File(planPath)
    if (!planFile.exists()) {
      echo("Error: Execution plan file not found: $planPath", err = true)
      System.exit(1)
    }

    try {
      val graph = json.decodeFromString<ExecutionGraph>(planFile.readText())
      validateGraph(graph)
      println("Execution plan is valid.")
    } catch (e: SerializationException) {
      echo("Error: Invalid execution plan format: ${e.message}", err = true)
      System.exit(1)
    } catch (e: IOException) {
      echo("Error: Could not read execution plan: ${e.message}", err = true)
      System.exit(1)
    }
  }

  private fun validateGraph(graph: ExecutionGraph) {
    val agentIds = graph.lanes.map { it.agentId }.toSet()
    graph.lanes.forEach { lane ->
      lane.dependencies.forEach { dep ->
        if (!agentIds.contains(dep)) {
          echo("Error: Agent ${lane.agentId} depends on unknown agent $dep", err = true)
          System.exit(1)
        }
      }
    }
  }
}
