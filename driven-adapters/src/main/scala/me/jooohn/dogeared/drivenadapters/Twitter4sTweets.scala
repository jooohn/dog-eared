package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.effect.{ContextShift, IO}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{Tweet => Twitter4sTweet}
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, Tweet, TwitterUserId}
import me.jooohn.dogeared.drivenports.Tweets

import scala.concurrent.ExecutionContext
import scala.util.Try

class Twitter4sTweets(restClient: TwitterRestClient)(implicit CS: ContextShift[IO], ec: ExecutionContext)
    extends Tweets[IO] {

  override def processNewTweets(twitterUserId: TwitterUserId)(f: List[Tweet] => IO[Unit]): IO[Unit] =
    for {
      tweets <- resolveTweets(twitterUserId)
      _ <- f(tweets)
    } yield ()

  private def resolveTweets(twitterUserId: TwitterUserId): IO[List[Tweet]] =
    IO.fromFuture(
        IO(
          restClient.userTimelineForUserId(
            twitterUserId.toLong,
            exclude_replies = true,
            include_rts = false,
          )))
      .map { readData =>
        readData.data.toList.flatMap(_.toTweet)
      }

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
