package me.jooohn.dogeared.drivenadapters

import cats.Monad
import cats.syntax.all._
import me.jooohn.dogeared.domain.{Tweet, TwitterUserId}
import me.jooohn.dogeared.drivenports.Tweets

import scala.collection.mutable.ListBuffer

class InMemoryTweets[F[_]: Monad](initialNewTweets: List[Tweet] = Nil) extends Tweets[F] {

  val newTweets = ListBuffer.from(initialNewTweets)

  override def processNewTweets(twitterUserId: TwitterUserId)(f: List[Tweet] => F[Unit]): F[Unit] =
    f(newTweets.toList) map { _ =>
      newTweets.clear()
      ()
    }

}
