package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.effect.{Async, ContextShift}
import cats.syntax.all._
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{Tweet => Twitter4sTweet}
import me.jooohn.dogeared.domain._
import me.jooohn.dogeared.drivenports.{ConcurrentIO, ProcessedTweets, Twitter}

import scala.util.Try

case class Twitter4STwitter[F[_]: Async: ContextShift](
    restClient: TwitterRestClient,
    processedTweets: ProcessedTweets[F],
    concurrentIO: ConcurrentIO[F])
    extends Twitter[F] {

  override def findUserTweets(twitterUserId: TwitterUserId, since: Option[TweetId]): F[List[Tweet]] =
    concurrentIO(
      Async
        .fromFuture(
          Async[F].delay(
            restClient.userTimelineForUserId(
              twitterUserId.toLong,
              since_id = since.map(_.toLong),
              exclude_replies = true,
              include_rts = false,
            )))
        .map(_.data.toList.flatMap(_.toTweet))
    )

  override def findUserAccount(twitterUserId: TwitterUserId): F[Option[TwitterUser]] =
    concurrentIO(Async.fromFuture(Async[F].delay(restClient.userById(twitterUserId.toLong))) map { data =>
      Some(
        TwitterUser(
          id = data.data.id_str,
          username = data.data.screen_name
        ))
    })

  override def findUserAccountByName(twitterUserName: TwitterUsername): F[Option[TwitterUser]] =
    concurrentIO(Async.fromFuture(Async[F].delay(restClient.user(twitterUserName))) map { data =>
      Some(
        TwitterUser(
          id = data.data.id_str,
          username = data.data.screen_name
        ))
    })

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
