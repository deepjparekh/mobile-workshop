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
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LeaseAcquire :
  CliktCommand(name = "lease-acquire", help = "Acquires a fenced lease for a repository.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val optRunId by option("--run-id", help = "Unique ID for this run").required()
  private val optOwnerId by option("--owner-id", help = "ID of the owner").required()
  private val optHost by option("--host", help = "Host identifier").required()
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
          runId = optRunId,
          ownerId = optOwnerId,
          ownerPid = ProcessHandle.current().pid(),
          host = optHost,
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
        echo("Error: Lock directory exists but lease.json is missing.", err = true)
        return
      }

      val currentLease: Lease =
        try {
          json.decodeFromString(leaseFile.readText())
        } catch (e: Exception) {
          echo("Error: Corrupted lease file.", err = true)
          return
        }

      val now = Clock.System.now()
      val expiresAt = Instant.parse(currentLease.expiresAt)

      if (now > expiresAt) {
        // Lease is expired. Check if owner is dead (macOS/Linux/Windows JVM 9+)
        val isOwnerAlive =
          currentLease.ownerPid?.let { pid -> ProcessHandle.of(pid).isPresent } ?: false

        if (!isOwnerAlive) {
          echo("Warning: Stale lock detected (expired and owner dead). Repairing...", err = true)
          // To repair, we overwrite the lease file with a new runId and ownerId
          val duration = (leaseDurationMin ?: 5).minutes
          val newLease =
            Lease(
              runId = optRunId,
              ownerId = optOwnerId,
              ownerPid = ProcessHandle.current().pid(),
              host = optHost,
              leaseEpoch = currentLease.leaseEpoch + 1,
              acquiredAt = now.toLocalDateTime(TimeZone.UTC).toString(),
              heartbeatAt = now.toLocalDateTime(TimeZone.UTC).toString(),
              expiresAt = (now + duration).toLocalDateTime(TimeZone.UTC).toString(),
            )
          writeLeaseFile(leaseFile, newLease)
          println(json.encodeToString(newLease))
          return
        }
      }

      echo("Error: Repository is already locked by ${currentLease.ownerId}.", err = true)
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
