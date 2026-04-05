package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

class ExtractError :
  CliktCommand(name = "extract-error", help = "Extracts an error excerpt from a log file.") {
  private val logPath by option("--log", help = "Path to the log file").required()
  private val maxLines = 200
  private val maxBytes = 16 * 1024

  override fun run() {
    val logFile = File(logPath)
    if (!logFile.exists()) {
      echo("Error: Log file not found: $logPath", err = true)
      System.exit(1)
    }

    val lines = logFile.readLines()
    if (lines.isEmpty()) {
      println("")
      return
    }

    val errorRegex = Regex("error|failed|exception|fatal", RegexOption.IGNORE_CASE)
    val firstErrorIdx = lines.indexOfFirst { errorRegex.containsMatchIn(it) }

    val excerptLines = mutableListOf<String>()

    if (firstErrorIdx != -1) {
      val start = (firstErrorIdx - 20).coerceAtLeast(0)
      val end = (firstErrorIdx + 20).coerceAtMost(lines.size - 1)
      for (i in start..end) {
        excerptLines.add(lines[i])
      }

      // Append final 120 lines if they don't overlap too much
      val finalStart = (lines.size - 120).coerceAtLeast(end + 1)
      for (i in finalStart until lines.size) {
        excerptLines.add(lines[i])
      }
    } else {
      // No match found, first 80 and final 120
      val firstEnd = 80.coerceAtMost(lines.size - 1)
      for (i in 0..firstEnd) {
        excerptLines.add(lines[i])
      }
      val finalStart = (lines.size - 120).coerceAtLeast(firstEnd + 1)
      for (i in finalStart until lines.size) {
        excerptLines.add(lines[i])
      }
    }

    // Cap at maxLines and maxBytes
    val cappedLines = excerptLines.takeLast(maxLines)
    var result = cappedLines.joinToString("\n")

    if (result.length > maxBytes) {
      result = result.takeLast(maxBytes)
    }

    println(result)
  }
}
