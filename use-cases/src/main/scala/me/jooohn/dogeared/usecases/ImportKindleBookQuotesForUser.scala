package me.jooohn.dogeared.usecases

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import me.jooohn.dogeared.domain._
import me.jooohn.dogeared.drivenports._

class ImportKindleBookQuotesForUser[F[_]: Monad](
    twitter: Twitter[F],
    twitterUsers: TwitterUsers[F],
    processedTweets: ProcessedTweets[F],
    kindleQuotePages: KindleQuotePages[F],
    kindleQuotedTweets: KindleQuotedTweets[F],
    kindleBooks: KindleBooks[F]
) {
  import ImportKindleBookQuotesForUser._

  private type QuotePageForTweet = Map[TweetId, KindleQuotePage]

  private val ensureTwitterUserExistence = new EnsureTwitterUserExistence(
    twitter = twitter,
    twitterUsers = twitterUsers,
  )

  def apply(twitterUserId: TwitterUserId): F[Either[Error, Unit]] =
    (for {
      _ <- EitherT(ensureTwitterUserExistence(twitterUserId))
      _ <- EitherT.right[Error](processNewTweets(twitterUserId)(importKindleBookQuotesFromTweets))
    } yield ()).value

  private def processNewTweets(twitterUserId: TwitterUserId)(f: List[Tweet] => F[Unit]): F[Unit] =
    for {
      processedTweet <- processedTweets.resolveByUserId(twitterUserId)
      newTweets <- twitter.findUserTweets(twitterUserId, processedTweet.map(_.latestProcessedTweetId))
      _ <- f(newTweets)
      _ <- newTweets.headOption.fold(Monad[F].unit)(latestTweet =>
        processedTweets.recordLatestProcessedTweetId(twitterUserId, latestTweet.id))
    } yield ()

  private def importKindleBookQuotesFromTweets(tweets: List[Tweet]): F[Unit] = {
    def resolveQuotesForTweet(tweets: List[Tweet]): F[Map[TweetId, KindleQuotePage]] =
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

    def resolveKindleQuotePages(urls: List[AmazonRedirectorURL])
      : F[(Map[AmazonRedirectorURL, KindleQuotePageResolutionError], Map[AmazonRedirectorURL, KindleQuotePage])] =
      kindleQuotePages.resolveManyByURLs(urls) map { quotePageResolutionByURL =>
        val (resolutionErrorByURL, quotePageByURL) = quotePageResolutionByURL.toList.partitionEither {
          case (url, result) => result.bimap((url, _), (url, _))
        }
        (resolutionErrorByURL.toMap, quotePageByURL.toMap)
      }

    for {
      quotePageForTweet <- resolveQuotesForTweet(tweets)
      _ <- kindleBooks.storeMany(quotePageForTweet.collectBooks)
      _ <- kindleQuotedTweets.storeMany(tweets.collectQuotedTweets(quotePageForTweet))
    } yield ()
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
object ImportKindleBookQuotesForUser {
  type Error = EnsureTwitterUserExistence.Error
}
