package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.Monad
import cats.effect.{Async, ContextShift}
import cats.syntax.all._
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{RatedData, Tweet => Twitter4sTweet}
import me.jooohn.dogeared.domain._
import me.jooohn.dogeared.drivenports.{ConcurrentExecutor, ProcessedTweets, Twitter}

import scala.concurrent.Future
import scala.util.Try

case class Twitter4STwitter[F[_]: Async: ContextShift](
    restClient: TwitterRestClient,
    processedTweets: ProcessedTweets[F],
    concurrentExecutor: ConcurrentExecutor[F])
    extends Twitter[F] {
  import Twitter4STwitter._

  override def findUserTweets(
      twitterUserId: TwitterUserId,
      since: Option[TweetId],
      count: Int = 1000): F[List[Tweet]] = {

    def fetch(future: => Future[RatedData[Seq[Twitter4sTweet]]]) =
      Async.fromFuture(Async[F].delay(future)).map(_.data.toList.flatMap(_.toTweet))

    def nextSince(tweets: List[Tweet]): (FetchStep, List[Tweet]) = {
      val sorted = tweets.sortBy(_.id)
      sorted.lastOption.fold[(FetchStep, List[Tweet])]((Terminal, sorted))(tweet => (Since(tweet.id.toLong), sorted))
    }

    def nextUntil(tweets: List[Tweet]): (FetchStep, List[Tweet]) = {
      val sorted = tweets.sortBy(_.id)
      sorted.headOption.fold[(FetchStep, List[Tweet])]((Terminal, sorted))(tweet => (Until(tweet.id.toLong), sorted))
    }

    def fetchNext(state: FetchStep): F[(FetchStep, List[Tweet])] = state match {
      case Since(id) =>
        fetch(
          restClient.userTimelineForUserId(
            twitterUserId.toLong,
            exclude_replies = true,
            since_id = Some(id),
            include_rts = false,
          )).map(nextSince)
      case Until(id) =>
        fetch(
          restClient.userTimelineForUserId(
            twitterUserId.toLong,
            exclude_replies = true,
            max_id = Some(id - 1),
            include_rts = false,
          )).map(nextUntil)
      case Latest =>
        fetch(
          restClient.userTimelineForUserId(
            twitterUserId.toLong,
            exclude_replies = true,
            include_rts = false,
          )).map(nextUntil)
      case Terminal => Monad[F].pure((Terminal, Nil))
    }

    def loop(result: List[Tweet], state: FetchStep): F[List[Tweet]] =
      fetchNext(state) flatMap {
        case (Terminal, tweets) => Monad[F].pure(tweets ::: result)
        case (nextStep, tweets) =>
          val nextResult = tweets ::: result
          if (nextResult.length > count) Monad[F].pure(nextResult)
          else loop(nextResult, nextStep)
      }
    concurrentExecutor.execute(loop(Nil, since.fold[FetchStep](Latest)(id => Since(id.toLong))).map(_.sortBy(_.id)))
  }

  override def findUserAccount(twitterUserId: TwitterUserId): F[Option[TwitterUser]] =
    concurrentExecutor.execute(Async.fromFuture(Async[F].delay(restClient.userById(twitterUserId.toLong))) map { data =>
      Some(
        TwitterUser(
          id = data.data.id_str,
          username = data.data.screen_name
        ))
    })

  override def findUserAccountByName(twitterUserName: TwitterUsername): F[Option[TwitterUser]] =
    concurrentExecutor.execute(Async.fromFuture(Async[F].delay(restClient.user(twitterUserName))) map { data =>
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

object Twitter4STwitter {

  sealed trait FetchStep
  case class Since(id: Long) extends FetchStep
  case class Until(id: Long) extends FetchStep
  case object Latest extends FetchStep
  case object Terminal extends FetchStep
}
