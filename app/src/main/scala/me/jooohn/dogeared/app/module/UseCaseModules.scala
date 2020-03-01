package me.jooohn.dogeared.app.module

import cats.effect.IO
import me.jooohn.dogeared.drivenports.{KindleBooks, KindleQuotePages, KindleQuotedTweets, Tweets}
import me.jooohn.dogeared.usecases.ImportKindleBookQuotes

trait UseCaseModules {
  val tweets: Tweets[IO]
  val kindleQuotePages: KindleQuotePages[IO]
  val kindleQuotedTweets: KindleQuotedTweets[IO]
  val kindleBooks: KindleBooks[IO]

  lazy val importKindleBookQuotes: ImportKindleBookQuotes[IO] = new ImportKindleBookQuotes[IO](
    tweets = tweets,
    kindleQuotePages = kindleQuotePages,
    kindleQuotedTweets = kindleQuotedTweets,
    kindleBooks = kindleBooks,
  )
}
