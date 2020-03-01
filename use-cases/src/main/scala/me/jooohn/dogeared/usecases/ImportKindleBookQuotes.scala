package me.jooohn.dogeared.usecases

import java.net.URL

import cats.Monad
import cats.implicits._
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, KindleBook, KindleQuotedTweet, Tweet, TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenports._

class ImportKindleBookQuotes[F[_]: Monad](
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

  private def resolveQuotesForTweet(tweets: List[Tweet]): F[Map[TweetId, KindleQuotePage]] =
    resolveKindleQuotePages(tweets.collectAmazonRedirectorLinkURLs) map {
      case (resolutionErrorByURL, quotePageByURL) =>
        resolutionErrorByURL.foreach {
          case (url, error) =>
            // TODO: Use sophisticated logging framework
            println(s"Failed to resolve as KindleQuotePage (url: ${url}, reason: ${error})")
        }
        (for {
          tweet <- tweets
          quotePage <- quotePageByURL.get(tweet.amazonRedirectorLinkURL)
        } yield (tweet.id, quotePage)).toMap
    }

  private def resolveKindleQuotePages(urls: List[AmazonRedirectorURL])
    : F[(Map[AmazonRedirectorURL, KindleQuotePageResolutionError], Map[AmazonRedirectorURL, KindleQuotePage])] =
    kindleQuotePages.resolveManyByURLs(urls) map { quotePageResolutionByURL =>
      val (resolutionErrorByURL, quotePageByURL) = quotePageResolutionByURL.toList.partitionEither {
        case (url, result) => result.bimap((url, _), (url, _))
      }
      (resolutionErrorByURL.toMap, quotePageByURL.toMap)
    }

  implicit class TweetListOps(tweets: List[Tweet]) {

    def collectQuotedTweets(quotePageForTweet: QuotePageForTweet): List[KindleQuotedTweet] =
      tweets.flatMap { tweet =>
        quotePageForTweet.get(tweet.id).map(page => tweet.kindleQuoted(page.quote))
      }
  }

  implicit class TweetsOps(tweets: List[Tweet]) {

    def collectAmazonRedirectorLinkURLs: List[AmazonRedirectorURL] = tweets map (_.amazonRedirectorLinkURL)

  }

  implicit class QuotePageForTweetOps(quotePageForTweet: QuotePageForTweet) {

    def collectBooks: List[KindleBook] = quotePageForTweet.values.map(_.book).toList

  }

}
