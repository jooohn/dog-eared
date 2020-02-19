package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.effect.{ContextShift, IO}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{Tweet => Twitter4sTweet}
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, Tweet, TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenports.{ProcessedTweets, Tweets}

import scala.util.Try

class Twitter4sTweets(restClient: TwitterRestClient, processedTweets: ProcessedTweets[IO])(
    implicit CS: ContextShift[IO])
    extends Tweets[IO] {

  override def processNewTweets(twitterUserId: TwitterUserId)(f: List[Tweet] => IO[Unit]): IO[Unit] =
    for {
      processedTweet <- processedTweets.resolveByUserId(twitterUserId)
      tweets <- resolveTweets(twitterUserId, sinceTweetId = processedTweet.map(_.lastProcessedTweetId))
      _ <- f(tweets)
      _ <- recordLastProcessedTweetId(twitterUserId, tweets.headOption.map(_.id))
    } yield ()

  private def resolveTweets(twitterUserId: TwitterUserId, sinceTweetId: Option[TweetId]): IO[List[Tweet]] =
    IO.fromFuture(
        IO(
          restClient.userTimelineForUserId(
            twitterUserId.toLong,
            since_id = sinceTweetId.map(_.toLong),
            exclude_replies = true,
            include_rts = false,
          )))
      .map { readData =>
        readData.data.toList.flatMap(_.toTweet)
      }

  private def recordLastProcessedTweetId(twitterUserId: TwitterUserId, lastProcessedTweet: Option[TweetId]): IO[Unit] =
    lastProcessedTweet.fold(IO.unit)(processedTweets.recordLastProcessedTweetId(twitterUserId, _))

  implicit class Twitter4sTweetOps(tweet: Twitter4sTweet) {

    def toTweet: Option[Tweet] =
      for {
        user <- tweet.user
        amazonRedirectorLink <- tweet.amazonRedirectorLink
      } yield
        Tweet(
          id = tweet.id_str,
          userId = user.id_str,
          amazonRedirectorLinkURL = amazonRedirectorLink
        )

    def amazonRedirectorLink: Option[AmazonRedirectorURL] =
      (for {
        entities <- tweet.entities.toList
        urlDetail <- entities.urls
        amazonRedirectorLink <- Try(new URL(urlDetail.expanded_url)).toOption.flatMap(AmazonRedirectorURL.fromURL)
      } yield amazonRedirectorLink).headOption

  }

}
