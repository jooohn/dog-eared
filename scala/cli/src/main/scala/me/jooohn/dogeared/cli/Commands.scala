package me.jooohn.dogeared.cli

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.AppDesign
import me.jooohn.dogeared.domain.TwitterUserId
import sun.misc.Signal
import zio.{ExitCode, RIO, ZIO}

class Commands(design: AppDesign) {
  import design._

  val importTweets: Command[RIO[zio.ZEnv, ExitCode]] =
    Command(
      name = "import-tweets",
      header = "Import tweets including Amazon kindle quotes"
    ) {
      Opts
        .option[TwitterUserId](
          "twitter-user-id",
          "Target twitter-user-id to import tweets. If not specified, import tweets for all registered users.")
        .orNone map {
        case Some(twitterUserId) =>
          importKindleBookQuotesForUser.compile.use { useCase =>
            useCase(twitterUserId) as ExitCode.success
          }
        case None =>
          importKindleBookQuotesForAllUsers.compile.use { useCase =>
            useCase.apply as ExitCode.success
          }
      }
    }

  val server: Command[RIO[zio.ZEnv, ExitCode]] =
    Command(
      name = "server",
      header = "Start HTTP server process"
    ) {
      Opts {
        design.server.compile.use { _ =>
          ZIO.effectAsync[Any, Nothing, Unit] { done =>
            Signal.handle(new Signal("INT"), _ => done(ZIO.unit))
          }
        } as ExitCode.success
      }
    }

}
