package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File
import kotlin.system.measureTimeMillis

class FilesystemSafetyProbe :
  CliktCommand(
    name = "filesystem-safety-probe",
    help = "Probes the filesystem for safety and performance.",
  ) {
  private val repoPath by option("--repo", help = "Path to the repository").required()

  override fun run() {
    val stateDir = File(repoPath, ".context/state")
    if (!stateDir.exists()) stateDir.mkdirs()

    val probeDir = File(stateDir, "probe-${System.currentTimeMillis()}")
    probeDir.mkdir()

    val latencies = mutableListOf<Long>()

    try {
      repeat(10) { i ->
        val time = measureTimeMillis {
          val file = File(probeDir, "test-$i.tmp")
          file.writeText("test")
          val renamedFile = File(probeDir, "test-$i.renamed")
          file.renameTo(renamedFile)
          if (!renamedFile.exists()) throw Exception("Metadata visibility error")
          renamedFile.delete()
        }
        latencies.add(time)
      }

      val avgLatency = latencies.average()
      if (avgLatency > 100) {
        echo("FILESYSTEM_SLOW: Average latency ${avgLatency}ms is too high.")
        System.exit(1)
      } else {
        echo("FILESYSTEM_OK: Average latency ${avgLatency}ms.")
      }
    } finally {
      probeDir.deleteRecursively()
    }
  }
}
