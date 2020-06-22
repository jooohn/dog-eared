package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.effect.{ContextShift, IO}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{Tweet => Twitter4sTweet}
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, Tweet, TweetId, TwitterUser, TwitterUserId}
import me.jooohn.dogeared.drivenports.{ConcurrentIO, ProcessedTweets, Twitter}

import scala.util.Try

case class Twitter4STwitter(
    restClient: TwitterRestClient,
    processedTweets: ProcessedTweets[IO],
    concurrentIO: ConcurrentIO[IO])(implicit CS: ContextShift[IO])
    extends Twitter[IO] {

  override def findUserTweets(twitterUserId: TwitterUserId, since: Option[TweetId]): IO[List[Tweet]] =
    concurrentIO {
      IO.fromFuture(
          IO(
            restClient.userTimelineForUserId(
              twitterUserId.toLong,
              since_id = since.map(_.toLong),
              exclude_replies = true,
              include_rts = false,
            )
          ))
        .map(_.data.toList.flatMap(_.toTweet))
    }

  override def findUserAccount(twitterUserId: TwitterUserId): IO[Option[TwitterUser]] =
    concurrentIO {
      IO.fromFuture(IO(restClient.userById(twitterUserId.toLong))) map { data =>
        Some(
          TwitterUser(
            id = twitterUserId,
            username = data.data.name
          ))
      }
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
