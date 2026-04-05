package dev.mobileworkshop.helper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import dev.mobileworkshop.helper.commands.AggregateRunState
import dev.mobileworkshop.helper.commands.CollectMetrics
import dev.mobileworkshop.helper.commands.ConfirmWorkspace
import dev.mobileworkshop.helper.commands.DiscoverWorkspace
import dev.mobileworkshop.helper.commands.ExtractError
import dev.mobileworkshop.helper.commands.FilesystemSafetyProbe
import dev.mobileworkshop.helper.commands.LeaseAcquire
import dev.mobileworkshop.helper.commands.LeaseRelease
import dev.mobileworkshop.helper.commands.LeaseRenew
import dev.mobileworkshop.helper.commands.UpdateAgentState
import dev.mobileworkshop.helper.commands.ValidateConfig
import dev.mobileworkshop.helper.commands.ValidatePlan

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
