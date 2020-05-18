package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.{ConcurrentEffect, ContextShift, IO}
import cats.effect.syntax._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}
import me.jooohn.dogeared.drivenadapters.dynamodb.DynamoErrorSyntax._
import me.jooohn.dogeared.drivenports.TwitterUsers
import org.scanamo.generic.auto._
import org.scanamo.syntax._
import org.scanamo.{ScanamoCats, Table}

case class DynamoTwitterUser(
    primaryKey: String,
    sortKey: String,
    data: String,
) {
  def toTwitterUser: TwitterUser = TwitterUser(
    id = primaryKey.stripPrefix("TWITTER_USER#"),
    username = data,
  )
}
object DynamoTwitterUser {

  def primaryKey(twitterUserId: TwitterUserId): String = s"TWITTER_USER#${twitterUserId}"
  def sortKey(twitterUserId: TwitterUserId, shardSize: Int): String =
    s"TWITTER_USER#${Shard.determine(twitterUserId, shardSize)}"

  def from(twitterUser: TwitterUser, shardSize: Int): DynamoTwitterUser = DynamoTwitterUser(
    primaryKey = primaryKey(twitterUser.id),
    sortKey = sortKey(twitterUser.id, shardSize),
    data = twitterUser.username,
  )
}

class DynamoTwitterUsers(scanamo: ScanamoCats[IO], logger: Logger[IO], shardSize: Int)(implicit CS: ContextShift[IO])
    extends TwitterUsers[IO] {
  val table: Table[DynamoTwitterUser] = Table[DynamoTwitterUser]("dog-eared-main")

  override def resolve(id: TwitterUserId): IO[Option[TwitterUser]] =
    logger.info(s"resolving twitter user ${id}") *>
      scanamo
        .exec(
          table.get(
            "primaryKey" -> DynamoTwitterUser.primaryKey(id) and "sortKey" -> DynamoTwitterUser.sortKey(id, shardSize)))
        .raiseIfError
        .map(_.map(_.toTwitterUser))

  override def resolveAll: IO[List[TwitterUser]] =
    logger.info(s"resolving all twitter users") *>
      (0 until shardSize).toList
        .parTraverse(
          shard =>
            scanamo
              .exec(table.filter("sortKey" -> s"TWITTER_USER#${shard}").scan())
              .raiseIfError
              .map(_.map(_.toTwitterUser)))
        .map(_.flatten)

  override def store(twitterUser: TwitterUser): IO[Unit] =
    logger.info(s"storing twitter user ${twitterUser.id} (@${twitterUser.username})") *>
      scanamo.exec(table.put(DynamoTwitterUser.from(twitterUser, shardSize)))

  override def storeMany(twitterUsers: List[TwitterUser]): IO[Unit] =
    logger.info(s"storing ${twitterUsers.length} twitter users") *>
      scanamo.exec(table.putAll(twitterUsers.map(DynamoTwitterUser.from(_, shardSize)).toSet))
}
