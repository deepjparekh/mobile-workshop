package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

class ExtractError :
  CliktCommand(name = "extract-error", help = "Extracts an error excerpt from a log file.") {
  private val logPath by option("--log", help = "Path to the log file").required()

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

    val excerptLines =
      if (firstErrorIdx != -1) {
        generateErrorExcerpt(lines, firstErrorIdx)
      } else {
        generateDefaultExcerpt(lines)
      }

    // Cap at MAX_LINES and MAX_BYTES
    val cappedLines = excerptLines.takeLast(MAX_LINES)
    var result = cappedLines.joinToString("\n")

    if (result.length > MAX_BYTES) {
      result = result.takeLast(MAX_BYTES)
    }

    println(result)
  }

  private fun generateErrorExcerpt(lines: List<String>, firstErrorIdx: Int): List<String> {
    val excerptLines = mutableListOf<String>()
    val start = (firstErrorIdx - CONTEXT_LINES).coerceAtLeast(0)
    val end = (firstErrorIdx + CONTEXT_LINES).coerceAtMost(lines.size - 1)
    for (i in start..end) {
      excerptLines.add(lines[i])
    }

    // Append final lines if they don't overlap too much
    val finalStart = (lines.size - FINAL_LINES).coerceAtLeast(end + 1)
    for (i in finalStart until lines.size) {
      excerptLines.add(lines[i])
    }
    return excerptLines
  }

  private fun generateDefaultExcerpt(lines: List<String>): List<String> {
    val excerptLines = mutableListOf<String>()
    val firstEnd = FIRST_LINES_DEFAULT.coerceAtMost(lines.size - 1)
    for (i in 0..firstEnd) {
      excerptLines.add(lines[i])
    }
    val finalStart = (lines.size - FINAL_LINES).coerceAtLeast(firstEnd + 1)
    for (i in finalStart until lines.size) {
      excerptLines.add(lines[i])
    }
    return excerptLines
  }

  companion object {
    private const val MAX_LINES = 200
    private const val MAX_BYTES = 16 * 1024
    private const val CONTEXT_LINES = 20
    private const val FINAL_LINES = 120
    private const val FIRST_LINES_DEFAULT = 80
  }
}
