package me.jooohn.dogeared.drivenadapters

import cats.Monad
import cats.effect.Bracket
import cats.implicits._
import doobie.Transactor
import doobie.implicits._
import me.jooohn.dogeared.domain.{TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenports.{ProcessedTweet, ProcessedTweets}

class PGProcessedTweets[F[_]: Monad: Transactor: Bracket[*[_], Throwable]] extends ProcessedTweets[F] {
  import instances.processedTweet._

  val transactor: Transactor[F] = implicitly[Transactor[F]]

  override def resolveByUserId(twitterUserId: TwitterUserId): F[Option[ProcessedTweet]] =
    sql"""
         |SELECT twitter_user_id, last_processed_tweet_id
         |FROM processed_tweets WHERE twitter_user_id = ${twitterUserId}
         |""".stripMargin
      .query[ProcessedTweet]
      .option
      .transact[F](transactor)

  override def recordLastProcessedTweetId(twitterUserId: TwitterUserId, tweetId: TweetId): F[Unit] =
    sql"""
         |INSERT INTO processed_tweets
         |(twitter_user_id, last_processed_tweet_id)
         |VALUES (${twitterUserId}, ${tweetId}) ON CONFLICT (tweet_user_id) DO UPDATE
         |SET last_processed_tweet_id = EXCLUDED.last_processed_tweet_id,
         |    updated_at              = CURRENT_TIMESTAMP
         |""".stripMargin.update.run.transact[F](transactor) *> Monad[F].unit
}
