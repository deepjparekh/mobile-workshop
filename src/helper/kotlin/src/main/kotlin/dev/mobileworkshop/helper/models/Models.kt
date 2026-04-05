package dev.mobileworkshop.helper.models

import kotlinx.serialization.Serializable

@Serializable
enum class ModuleKind {
  KMP_SHARED,
  ANDROID_APP,
  ANDROID_LIBRARY,
  IOS_APP,
  BACKEND_SERVICE,
  UNKNOWN,
  AMBIGUOUS,
}

@Serializable
data class WorkspaceModule(
  val path: String,
  val kind: ModuleKind,
  val confidence: Double,
  val evidence: List<String>,
  val buildTools: List<String> = emptyList(),
  val ownedBy: List<String> = emptyList(),
)

@Serializable
data class WorkspaceDetection(val modules: List<WorkspaceModule>, val detectedAt: String)

@Serializable
data class Lease(
  val runId: String,
  val ownerId: String,
  val ownerPid: Long?,
  val host: String,
  val leaseEpoch: Long,
  val acquiredAt: String,
  val heartbeatAt: String,
  val expiresAt: String,
)

@Serializable
data class AgentState(
  val agentId: String,
  val status: String, // idle, running, completed, failed
  val lastMessageAt: String?,
  val currentTask: String?,
)

@Serializable
data class RunState(
  val runId: String,
  val status: String, // running, completed, failed, partially_failed
  val startedAt: String,
  val updatedAt: String,
  val agents: Map<String, AgentState>,
)
