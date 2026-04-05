package dev.mobileworkshop.helper.models

import kotlinx.serialization.Serializable

@Serializable
data class ExecutionLane(
  val agentId: String,
  val tasks: List<String>,
  val dependencies: List<String> = emptyList(),
)

@Serializable data class ExecutionGraph(val runId: String, val lanes: List<ExecutionLane>)
