package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import dev.mobileworkshop.helper.models.Lease
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
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

    if (lockDir.mkdir()) {
      acquireNewLock(leaseFile)
    } else {
      handleExistingLock(leaseFile)
    }
  }

  private fun acquireNewLock(leaseFile: File) {
    val now = Clock.System.now()
    val duration = (leaseDurationMin ?: DEFAULT_DURATION_MIN).minutes
    val lease =
      Lease(
        runId = optRunId,
        ownerId = optOwnerId,
        ownerPid = ProcessHandle.current().pid(),
        host = optHost,
        leaseEpoch = INITIAL_EPOCH,
        acquiredAt = now.toLocalDateTime(TimeZone.UTC).toString(),
        heartbeatAt = now.toLocalDateTime(TimeZone.UTC).toString(),
        expiresAt = (now + duration).toLocalDateTime(TimeZone.UTC).toString(),
      )

    writeLeaseFile(leaseFile, lease)
    println(json.encodeToString(lease))
  }

  private fun handleExistingLock(leaseFile: File) {
    if (!leaseFile.exists()) {
      echo("Error: Lock directory exists but lease.json is missing.", err = true)
      return
    }

    val currentLease: Lease? =
      try {
        json.decodeFromString<Lease>(leaseFile.readText())
      } catch (e: SerializationException) {
        echo("Error: Corrupted lease file: ${e.message}", err = true)
        null
      } catch (e: IOException) {
        echo("Error: Could not read lease file: ${e.message}", err = true)
        null
      }

    if (currentLease == null) return

    val now = Clock.System.now()
    val expiresAt = Instant.parse(currentLease.expiresAt)

    if (now > expiresAt && isOwnerDead(currentLease)) {
      repairStaleLock(leaseFile, currentLease)
    } else {
      echo("Error: Repository is already locked by ${currentLease.ownerId}.", err = true)
    }
  }

  private fun isOwnerDead(lease: Lease): Boolean {
    return lease.ownerPid?.let { pid -> ProcessHandle.of(pid).isEmpty } ?: true
  }

  private fun repairStaleLock(leaseFile: File, currentLease: Lease) {
    echo("Warning: Stale lock detected (expired and owner dead). Repairing...", err = true)
    val now = Clock.System.now()
    val duration = (leaseDurationMin ?: DEFAULT_DURATION_MIN).minutes
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

  companion object {
    private const val DEFAULT_DURATION_MIN = 5L
    private const val INITIAL_EPOCH = 1L
  }
}
