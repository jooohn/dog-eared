package me.jooohn.dogeared.cli

import cats.effect.concurrent.MVar
import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, Timer}
import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.AppDesign
import me.jooohn.dogeared.domain.TwitterUserId
import sun.misc.Signal

class Commands(implicit val cs: ContextShift[IO], val timer: Timer[IO], val zioRuntime: zio.Runtime[zio.ZEnv]) {
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
        design.server.compile.use { _ =>
          for {
            terminate <- MVar[IO](IO.ioConcurrentEffect(contextShift)).empty[Unit]
            _ <- IO(Signal.handle(new Signal("INT"), _ => terminate.put(()).unsafeRunAsyncAndForget()))
            _ <- terminate.read
          } yield ()
        } as ExitCode.Success
      }
    }

}
