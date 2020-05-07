package me.jooohn.dogeared.app.module

import cats.effect.IO
import me.jooohn.dogeared.drivenports.{
  KindleBooks,
  KindleQuotePages,
  KindleQuotedTweets,
  ProcessedTweets,
  Twitter,
  TwitterUsers
}
import me.jooohn.dogeared.usecases.ImportKindleBookQuotesForUser

trait UseCaseModules {
  val tweets: Twitter[IO]
  val twitterUsers: TwitterUsers[IO]
  val kindleQuotePages: KindleQuotePages[IO]
  val kindleQuotedTweets: KindleQuotedTweets[IO]
  val kindleBooks: KindleBooks[IO]
  val processedTweets: ProcessedTweets[IO]

  lazy val importKindleBookQuotes: ImportKindleBookQuotesForUser[IO] = new ImportKindleBookQuotesForUser[IO](
    twitter = tweets,
    twitterUsers = twitterUsers,
    kindleQuotePages = kindleQuotePages,
    kindleQuotedTweets = kindleQuotedTweets,
    kindleBooks = kindleBooks,
    processedTweets = processedTweets,
  )
}
