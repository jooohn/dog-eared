package me.jooohn.dogeared.cli

import cats.implicits._
import com.monovore.decline._
import zio.{ExitCode, URIO}

abstract class CommandZIOApp(
    name: String,
    header: String,
    helpFlag: Boolean = true,
    version: String = ""
) extends zio.interop.catz.CatsApp {

  def main: Opts[URIO[zio.ZEnv, ExitCode]]

  private[this] def command: Command[URIO[zio.ZEnv, ExitCode]] = {
    val showVersion = {
      if (version.isEmpty) Opts.never
      else {
        val flag = Opts.flag(
          long = "version",
          short = "v",
          help = "Print the version number and exit.",
          visibility = Visibility.Partial
        )
        flag.as(URIO(System.out.println(version)).as(ExitCode.success))
      }
    }

    Command(name, header, helpFlag)(showVersion orElse main)
  }

  override final def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    def printHelp(help: Help): URIO[zio.ZEnv, ExitCode] =
      URIO(System.err.println(help)).as {
        if (help.errors.nonEmpty) ExitCode.failure
        else ExitCode.success
      }

    for {
      parseResult <- URIO(command.parse(PlatformApp.ambientArgs getOrElse args, sys.env))
      exitCode <- parseResult.fold(printHelp, identity)
    } yield exitCode
  }

}
