package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import dev.mobileworkshop.helper.models.Lease
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LeaseAcquire :
  CliktCommand(name = "lease-acquire", help = "Acquires a fenced lease for a repository.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val runId by option("--run-id", help = "Unique ID for this run").required()
  private val ownerId by option("--owner-id", help = "ID of the owner").required()
  private val host by option("--host", help = "Host identifier").required()
  private val leaseDurationMin by option("--duration", help = "Lease duration in minutes").long()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val contextDir = File(repoPath, ".context/state")
    val lockDir = File(contextDir, "lock")
    val leaseFile = File(lockDir, "lease.json")

    if (!contextDir.exists()) contextDir.mkdirs()

    // Atomic directory creation for lock
    if (lockDir.mkdir()) {
      // We acquired the lock
      val now = Clock.System.now()
      val duration = (leaseDurationMin ?: 5).minutes
      val lease =
        Lease(
          runId = runId,
          ownerId = ownerId,
          ownerPid = ProcessHandle.current().pid(),
          host = host,
          leaseEpoch = 1,
          acquiredAt = now.toLocalDateTime(TimeZone.UTC).toString(),
          heartbeatAt = now.toLocalDateTime(TimeZone.UTC).toString(),
          expiresAt = (now + duration).toLocalDateTime(TimeZone.UTC).toString(),
        )

      writeLeaseFile(leaseFile, lease)
      println(json.encodeToString(lease))
    } else {
      // Lock dir exists, check lease file
      if (!leaseFile.exists()) {
        // This is a weird state, maybe lock dir was created but lease file not written?
        // For safety, we fail.
        echo("Error: Lock directory exists but lease.json is missing.", err = true)
        System.exit(1)
      }

      // In a real scenario we'd check for stale lease here, but the plan says
      // "If ownership cannot be proven stale, V1 fails closed"
      // Let's just fail for now as "already locked"
      echo("Error: Repository is already locked.", err = true)
      System.exit(1)
    }
  }

  private fun writeLeaseFile(file: File, lease: Lease) {
    val tempFile = File.createTempFile("lease", ".json", file.parentFile)
    tempFile.writeText(json.encodeToString(lease))
    Files.move(
      tempFile.toPath(),
      file.toPath(),
      StandardCopyOption.ATOMIC_MOVE,
      StandardCopyOption.REPLACE_EXISTING,
    )
  }
}
