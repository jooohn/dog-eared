package me.jooohn.dogeared.cli

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import me.jooohn.dogeared.cli.command._

object Main
    extends CommandIOApp(
      name = "dog-eared",
      header = "Dog Eared",
    ) {
  override def main: Opts[IO[ExitCode]] =
    Opts.subcommands(
      importTweets
    )
}
