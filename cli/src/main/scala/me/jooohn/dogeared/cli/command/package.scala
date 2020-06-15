package me.jooohn.dogeared.cli

import caliban.CalibanError.ExecutionError
import cats.effect.{ContextShift, ExitCode, IO, Timer}
import caliban.interop.cats.implicits._
import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.ProductionApp
import me.jooohn.dogeared.domain.TwitterUserId
import me.jooohn.dogeared.graphql.GraphQL
import me.jooohn.dogeared.server.HttpService
import org.http4s.server.blaze.BlazeServerBuilder
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.system.System

package object command {
  type ZEnv = Clock with Console with System with Random with Blocking
  implicit val runtime: zio.Runtime[ZEnv] = zio.Runtime.default

  def importTweets(implicit CS: ContextShift[IO], T: Timer[IO]): Command[IO[ExitCode]] =
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
          ProductionApp(_.importKindleBookQuotesForUser(twitterUserId)) as ExitCode.Success
        case None =>
          ProductionApp(_.importKindleBookQuotesForAllUsers.apply) as ExitCode.Success
      }
    }

  def server(implicit CS: ContextShift[IO], T: Timer[IO]): Command[IO[ExitCode]] =
    Command(
      name = "server",
      header = "Start HTTP server process"
    ) {
      Opts
        .option[Int]("port", "Port to bind the http server", short = "p")
        .orNone
        .map { portOpt =>
          ProductionApp { app =>
            for {
              interpreter <- GraphQL.interpreter[IO, ZEnv](
                twitterUserQueries = app.twitterUserQueries,
                kindleQuotedTweetQueries = app.kindleQuotedTweetQueries,
                kindleBookQueries = app.kindleBookQueries,
                logger = app.logger,
              )
              _ <- BlazeServerBuilder[IO](scala.concurrent.ExecutionContext.global)
                .bindHttp(portOpt.getOrElse(8080), "localhost")
                .withHttpApp(HttpService(interpreter))
                .resource
                .use(_ => IO(scala.io.StdIn.readLine()))
            } yield ()
          } as ExitCode.Success
        }
    }
}
