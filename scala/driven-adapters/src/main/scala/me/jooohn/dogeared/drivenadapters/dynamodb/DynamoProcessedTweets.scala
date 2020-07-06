package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.Sync
import cats.implicits._
import me.jooohn.dogeared.domain.{TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenadapters.dynamodb.DynamoErrorSyntax._
import me.jooohn.dogeared.drivenports.{Logger, ProcessedTweet, ProcessedTweets}
import org.scanamo.generic.auto._
import org.scanamo.syntax._
import org.scanamo.{ScanamoCats, Table}

case class DynamoProcessedTweet(
    primaryKey: String,
    sortKey: String,
    data: String,
) {

  def toProcessedTweet: ProcessedTweet = ProcessedTweet(
    twitterUserId = primaryKey.stripPrefix("TWITTER_USER#"),
    latestProcessedTweetId = data
  )

}

object DynamoProcessedTweet {

  def primaryKey(twitterUserId: TwitterUserId): String = s"TWITTER_USER#${twitterUserId}"
  def sortKey(twitterUserId: TwitterUserId, shardSize: Shard.Size): String =
    s"PROCESSED_TWEET#${Shard.determine(twitterUserId, shardSize)}"

  def from(twitterUserId: TwitterUserId, latestProcessedTweetId: TweetId, shardSize: Shard.Size): DynamoProcessedTweet =
    DynamoProcessedTweet(
      primaryKey = primaryKey(twitterUserId),
      sortKey = sortKey(twitterUserId, shardSize),
      data = latestProcessedTweetId,
    )
}

case class DynamoProcessedTweets[F[_]: Sync](scanamo: ScanamoCats[F], logger: Logger, shardSize: Shard.Size)
    extends ProcessedTweets[F] {
  val table: Table[DynamoProcessedTweet] = Table[DynamoProcessedTweet]("dog-eared-main")

  override def resolveByUserId(twitterUserId: TwitterUserId): F[Option[ProcessedTweet]] =
    logger.info(s"resolving processed tweets for twitter user ${twitterUserId}") *> scanamo
      .exec(table.get("primaryKey" -> DynamoProcessedTweet
        .primaryKey(twitterUserId) and "sortKey" -> DynamoProcessedTweet.sortKey(twitterUserId, shardSize)))
      .raiseIfError
      .map(_.map(_.toProcessedTweet)) <* logger.info(s"resolved")

  override def recordLatestProcessedTweetId(twitterUserId: TwitterUserId, tweetId: TweetId): F[Unit] =
    logger.info(s"recording ${tweetId} as the latest processed tweet for twitter user ${twitterUserId}") *> scanamo
      .exec(
        table.put(
          DynamoProcessedTweet.from(
            twitterUserId = twitterUserId,
            latestProcessedTweetId = tweetId,
            shardSize = shardSize
          )))
}
