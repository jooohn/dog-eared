package me.jooohn.dogeared.cli

import cats.effect.{ContextShift, ExitCode, IO}
import cats.implicits._
import com.monovore.decline.{Command, Opts}
import me.jooohn.dogeared.app.ProductionApp
import me.jooohn.dogeared.domain.TwitterUserId

package object command {

  def importTweets(implicit CS: ContextShift[IO]): Command[IO[ExitCode]] =
    Command(
      name = "import-tweets",
      header = "Import tweets including Amazon kindle quotes"
    ) {
      Opts.argument[TwitterUserId]("twitter-user-id") map { twitterUserId =>
        ProductionApp { container =>
          container.importKindleBookQuotes(twitterUserId)
        } as ExitCode.Success
      }
    }
}
