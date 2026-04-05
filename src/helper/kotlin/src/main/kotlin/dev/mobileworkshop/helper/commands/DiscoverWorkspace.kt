package dev.mobileworkshop.helper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.mobileworkshop.helper.models.ModuleKind
import dev.mobileworkshop.helper.models.WorkspaceDetection
import dev.mobileworkshop.helper.models.WorkspaceModule
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DiscoverWorkspace :
  CliktCommand(name = "discover-workspace", help = "Discovers modules in a repository.") {
  private val repoPath by option("--repo", help = "Path to the repository").required()
  private val json = Json { prettyPrint = true }

  override fun run() {
    val root = File(repoPath)
    if (!root.exists() || !root.isDirectory) {
      echo("Error: Invalid repository path: $repoPath", err = true)
      return
    }

    val modules = mutableListOf<WorkspaceModule>()
    val skipDirs = setOf(".git", "node_modules", "build", ".gradle", ".context", ".mobile-workshop")

    Files.walk(root.toPath(), MAX_DEPTH).forEach { path ->
      val fileName = path.name
      if (skipDirs.contains(fileName)) {
        return@forEach
      }
      if (Files.isDirectory(path)) {
        val relativePath = root.toPath().relativize(path)
        if (relativePath.any { skipDirs.contains(it.name) }) {
          return@forEach
        }

        val module = detectModule(path, root.toPath())
        if (module != null) {
          modules.add(module)
        }
      }
    }

    val detection =
      WorkspaceDetection(
        modules = modules,
        detectedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString(),
      )

    println(json.encodeToString(detection))
  }

  private fun detectModule(path: Path, root: Path): WorkspaceModule? {
    val dir = path.toFile()
    val files = dir.listFiles()?.map { it.name } ?: emptyList()
    val relativePath = root.relativize(path).toString().takeIf { it.isNotEmpty() } ?: "."

    return detectGradleModule(dir, files, relativePath) ?: detectIosModule(files, relativePath)
  }

  private fun detectGradleModule(
    dir: File,
    files: List<String>,
    relativePath: String,
  ): WorkspaceModule? {
    if (!files.contains("build.gradle.kts") && !files.contains("build.gradle")) return null

    val content =
      File(dir, "build.gradle.kts").takeIf { it.exists() }?.readText()
        ?: File(dir, "build.gradle").takeIf { it.exists() }?.readText()
        ?: ""

    return when {
      content.contains("kotlin(\"multiplatform\")") ||
        content.contains("org.jetbrains.kotlin.multiplatform") -> {
        WorkspaceModule(
          path = relativePath,
          kind = ModuleKind.KMP_SHARED,
          confidence = HIGH_CONFIDENCE,
          evidence = listOf("build.gradle(.kts) with KMP plugin"),
          buildTools = listOf("gradle"),
        )
      }
      content.contains("com.android.application") -> {
        WorkspaceModule(
          path = relativePath,
          kind = ModuleKind.ANDROID_APP,
          confidence = HIGH_CONFIDENCE,
          evidence = listOf("build.gradle(.kts) with Android application plugin"),
          buildTools = listOf("gradle"),
        )
      }
      content.contains("com.android.library") -> {
        WorkspaceModule(
          path = relativePath,
          kind = ModuleKind.ANDROID_LIBRARY,
          confidence = HIGH_CONFIDENCE,
          evidence = listOf("build.gradle(.kts) with Android library plugin"),
          buildTools = listOf("gradle"),
        )
      }
      else -> null
    }
  }

  private fun detectIosModule(files: List<String>, relativePath: String): WorkspaceModule? {
    val isIos =
      files.any { it.endsWith(".xcodeproj") || it.endsWith(".xcworkspace") } ||
        files.contains("Package.swift")
    if (!isIos) return null

    val evidenceFiles =
      files.filter {
        it.endsWith(".xcodeproj") || it.endsWith(".xcworkspace") || it == "Package.swift"
      }
    return WorkspaceModule(
      path = relativePath,
      kind = ModuleKind.IOS_APP,
      confidence = HIGH_CONFIDENCE,
      evidence = listOf("iOS project found: ${evidenceFiles.joinToString()}"),
      buildTools = listOf("xcodebuild"),
    )
  }

  companion object {
    private const val HIGH_CONFIDENCE = 0.9
    private const val MAX_DEPTH = 5
  }
}
