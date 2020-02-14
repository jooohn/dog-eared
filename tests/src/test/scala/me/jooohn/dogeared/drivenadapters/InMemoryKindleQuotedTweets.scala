package me.jooohn.dogeared.drivenadapters

import cats.Monad
import me.jooohn.dogeared.domain.{KindleQuotedTweet, TweetId}
import me.jooohn.dogeared.drivenports.KindleQuotedTweets

class InMemoryKindleQuotedTweets[F[_]: Monad] extends KindleQuotedTweets[F] {

  val stored = scala.collection.mutable.Map.empty[TweetId, KindleQuotedTweet]

  override def storeMany(kindleQuotedTweets: List[KindleQuotedTweet]): F[Unit] = {
    kindleQuotedTweets.foreach { kqt =>
      stored.update(kqt.tweetId, kqt)
    }
    Monad[F].unit
  }

}
