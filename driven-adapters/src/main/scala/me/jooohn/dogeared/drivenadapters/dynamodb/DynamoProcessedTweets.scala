package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.IO
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.domain.{TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenadapters.dynamodb.DynamoErrorSyntax._
import me.jooohn.dogeared.drivenports.{ProcessedTweet, ProcessedTweets}
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
  def from(twitterUserId: String, latestProcessedTweetId: String, shardSize: Int): DynamoProcessedTweet =
    DynamoProcessedTweet(
      primaryKey = s"TWITTER_USER#${twitterUserId}",
      sortKey = s"PROCESSED_TWEET#${Shard.determine(twitterUserId, shardSize)}",
      data = latestProcessedTweetId,
    )
}

class DynamoProcessedTweets(scanamo: ScanamoCats[IO], logger: Logger[IO], shardSize: Int) extends ProcessedTweets[IO] {
  val table: Table[DynamoProcessedTweet] = Table[DynamoProcessedTweet]("dog-eared-main")

  override def resolveByUserId(twitterUserId: TwitterUserId): IO[Option[ProcessedTweet]] =
    logger.info(s"resolving processed tweets for twitter user ${twitterUserId}") *> scanamo
      .exec(table.get("twitterUserId" -> twitterUserId and "type" -> "PROCESSED_TWEETS"))
      .raiseIfError
      .map(_.map(_.toProcessedTweet))

  override def recordLatestProcessedTweetId(twitterUserId: TwitterUserId, tweetId: TweetId): IO[Unit] =
    logger.info(s"recording ${tweetId} as the latest processed tweet for twitter user ${twitterUserId}") *> scanamo
      .exec(
        table.put(
          DynamoProcessedTweet.from(
            twitterUserId = twitterUserId,
            latestProcessedTweetId = tweetId,
            shardSize = shardSize
          )))
}
