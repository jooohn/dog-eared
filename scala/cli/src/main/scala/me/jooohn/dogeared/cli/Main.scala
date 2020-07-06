package me.jooohn.dogeared.cli

import com.monovore.decline.Opts
import me.jooohn.dogeared.app.AppDesign
import zio.interop.catz._
import zio.{ExitCode, URIO}

object Main
    extends CommandZIOApp(
      name = "dog-eared",
      header = "Dog Eared",
    ) {
  override def main: Opts[URIO[zio.ZEnv, ExitCode]] = {
    val commands = new Commands(AppDesign.apply)
    Opts.subcommands(
      commands.importTweets,
      commands.server,
    ) map (_.catchAll(e => (URIO(System.err.println(e)) as ExitCode.failure)))
  }
}
