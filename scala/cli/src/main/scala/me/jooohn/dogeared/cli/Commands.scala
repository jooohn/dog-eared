package me.jooohn.dogeared.cli

import cats.effect.{ContextShift, ExitCode, IO, Timer}
import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.AppDesign
import me.jooohn.dogeared.domain.TwitterUserId

class Commands(
    implicit val contextShift: ContextShift[IO],
    val timer: Timer[IO],
    val zioRuntime: zio.Runtime[zio.ZEnv]) {
  val design: AppDesign = AppDesign.apply

  val importTweets: Command[IO[ExitCode]] =
    Command(
      name = "import-tweets",
      header = "Import tweets including Amazon kindle quotes"
    ) {
      import design._
      Opts
        .option[TwitterUserId](
          "twitter-user-id",
          "Target twitter-user-id to import tweets. If not specified, import tweets for all registered users.")
        .orNone map {
        case Some(twitterUserId) =>
          importKindleBookQuotesForUser.compile.use { useCase =>
            useCase(twitterUserId) as ExitCode.Success
          }
        case None =>
          importKindleBookQuotesForAllUsers.compile.use { useCase =>
            useCase.apply as ExitCode.Success
          }
      }
    }

  val server: Command[IO[ExitCode]] =
    Command(
      name = "server",
      header = "Start HTTP server process"
    ) {
      import design._
      Opts {
        design.server.compile.use(_ => IO.never) as ExitCode.Success
      }
    }

}
