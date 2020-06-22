package me.jooohn.dogeared.cli

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object Main
    extends CommandIOApp(
      name = "dog-eared",
      header = "Dog Eared",
    ) {
  implicit val zioRuntime: zio.Runtime[zio.ZEnv] = zio.Runtime.default

  override def main: Opts[IO[ExitCode]] = {
    val commands = new Commands()
    Opts.subcommands(
      commands.importTweets,
      commands.server,
    )
  }
}
