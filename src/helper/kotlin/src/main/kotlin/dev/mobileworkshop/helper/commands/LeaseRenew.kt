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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LeaseRenew : CliktCommand(name = "lease-renew", help = "Renews a fenced lease.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val optRunId by option("--run-id", help = "Run ID").required()
  private val optOwnerId by option("--owner-id", help = "Owner ID").required()
  private val optLeaseEpoch by option("--epoch", help = "Current lease epoch").long().required()
  private val leaseDurationMin by option("--duration", help = "Lease duration in minutes").long()

  private val json = Json { prettyPrint = true }

  override fun run() {
    val lockDir = File(repoPath, ".context/state/lock")
    val leaseFile = File(lockDir, "lease.json")

    if (!leaseFile.exists()) {
      echo("Error: No lease found.", err = true)
      System.exit(1)
    }

    val currentLease: Lease =
      try {
        json.decodeFromString<Lease>(leaseFile.readText())
      } catch (e: Exception) {
        echo("Error: Corrupted lease file: ${e.message}", err = true)
        throw RuntimeException("Lease file corrupted", e)
      }

    if (
      currentLease.runId != optRunId ||
        currentLease.ownerId != optOwnerId ||
        currentLease.leaseEpoch != optLeaseEpoch
    ) {
      echo("Error: Lease ownership mismatch or epoch mismatch.", err = true)
      System.exit(1)
    }

    val now = Clock.System.now()
    val duration = (leaseDurationMin ?: 5).minutes
    val newLease =
      currentLease.copy(
        leaseEpoch = currentLease.leaseEpoch + 1,
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
}
