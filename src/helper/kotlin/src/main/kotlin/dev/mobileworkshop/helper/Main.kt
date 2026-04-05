package dev.mobileworkshop.helper

fun main(args: Array<String>) {
  val command = args.firstOrNull() ?: "help"
  println("mobile-workshop helper scaffold: command='$command'")
}
