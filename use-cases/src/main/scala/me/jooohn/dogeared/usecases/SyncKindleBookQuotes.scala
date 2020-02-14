package me.jooohn.dogeared.usecases

import cats.Monad
import cats.implicits._
import me.jooohn.dogeared.domain.{KindleBook, KindleQuotedTweet, Tweet, TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenports._

class SyncKindleBookQuotes[F[_]: Monad](
    tweets: Tweets[F],
    kindleQuotePages: KindleQuotePages[F],
    kindleQuotedTweets: KindleQuotedTweets[F],
    kindleBooks: KindleBooks[F]
) {
  private type QuotePageForTweet = Map[TweetId, KindleQuotePage]

  def apply(twitterUserId: TwitterUserId): F[Unit] =
    tweets.processNewTweets(twitterUserId) { unprocessedTweets =>
      for {
        quotePageForTweet <- resolveQuotesForTweet(unprocessedTweets)
        quotedTweets = unprocessedTweets.collectQuotedTweets(quotePageForTweet)
        _ <- kindleBooks.storeMany(quotePageForTweet.collectBooks)
        _ <- kindleQuotedTweets.storeMany(quotedTweets)
      } yield ()
    }

  private def resolveQuotesForTweet(tweets: List[Tweet]): F[Map[TweetId, KindleQuotePage]] = {
    val targetURLs = tweets.flatMap(_.linkToAmazon)
    kindleQuotePages.resolveManyByURLs(targetURLs) map { quotePageByURL =>
      (for {
        tweet <- tweets
        linkToAmazon <- tweet.linkToAmazon
        quotePage <- quotePageByURL.get(linkToAmazon)
      } yield (tweet.id, quotePage)).toMap
    }
  }

  implicit class TweetListOps(tweets: List[Tweet]) {

    def collectQuotedTweets(quotePageForTweet: QuotePageForTweet): List[KindleQuotedTweet] =
      tweets.flatMap { tweet =>
        quotePageForTweet.get(tweet.id).map(page => tweet.kindleQuoted(page.quote))
      }
  }

  implicit class QuotePageForTweetOps(quotePageForTweet: QuotePageForTweet) {

    def collectBooks: List[KindleBook] = quotePageForTweet.values.map(_.book).toList

  }

}
