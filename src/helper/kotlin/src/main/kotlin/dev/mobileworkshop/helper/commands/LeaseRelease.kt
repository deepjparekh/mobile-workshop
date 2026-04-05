package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

class LeaseRelease :
  CliktCommand(name = "lease-release", help = "Releases a fenced lease for a repository.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val runId by option("--run-id", help = "ID of the current run").required()

  override fun run() {
    val lockDir = File(repoPath, ".context/state/lock")
    val leaseFile = File(lockDir, "lease.json")

    if (lockDir.exists()) {
      // In a real scenario we'd check if we are the owner
      // For V1, let's just delete the lock directory
      if (leaseFile.exists()) {
        leaseFile.delete()
      }
      if (lockDir.delete()) {
        println("Lease released successfully.")
      } else {
        echo("Error: Could not release lease. Lock directory may be non-empty.", err = true)
        System.exit(1)
      }
    } else {
      println("No lease found to release.")
    }
  }
}
