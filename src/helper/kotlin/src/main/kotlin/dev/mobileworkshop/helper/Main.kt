package dev.mobileworkshop.helper

import com.github.ajalt.clikt.core.*
import dev.mobileworkshop.helper.commands.*

class Helper : CliktCommand(name = "mobile-workshop-helper") {
  override fun run() = Unit
}

fun main(args: Array<String>) =
  Helper()
    .subcommands(
      DiscoverWorkspace(),
      LeaseAcquire(),
      LeaseRenew(),
      LeaseRelease(),
      ExtractError(),
      FilesystemSafetyProbe(),
      ConfirmWorkspace(),
      UpdateAgentState(),
      AggregateRunState(),
      CollectMetrics(),
      ValidatePlan(),
      ValidateConfig(),
    )
    .main(args)
