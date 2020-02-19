package me.jooohn.dogeared.app.module

import cats.effect.IO
import me.jooohn.dogeared.usecases.ImportKindleBookQuotes
import com.softwaremill.macwire._
import me.jooohn.dogeared.drivenports.{KindleBooks, KindleQuotePages, KindleQuotedTweets, Tweets}

trait UseCaseModules {
  val tweets: Tweets[IO]
  val kindleQuotePages: KindleQuotePages[IO]
  val kindleQuotedTweets: KindleQuotedTweets[IO]
  val kindleBooks: KindleBooks[IO]

  lazy val importKindleBookQuotes: ImportKindleBookQuotes[IO] = wire[ImportKindleBookQuotes[IO]]
}
