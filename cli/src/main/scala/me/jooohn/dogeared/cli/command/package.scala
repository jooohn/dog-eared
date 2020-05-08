package me.jooohn.dogeared.cli

import cats.effect.{ContextShift, ExitCode, IO, Timer}
import cats.implicits._
import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.ProductionApp
import me.jooohn.dogeared.domain.TwitterUserId

package object command {

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
}
