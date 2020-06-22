package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.ContextShift
import cats.implicits._
import cats.{MonadError, Parallel}
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}
import me.jooohn.dogeared.drivenadapters.dynamodb.DynamoTwitterUser.findByShardOp
import me.jooohn.dogeared.drivenports.{TwitterUserQueries, TwitterUsers}
import org.scanamo.generic.auto._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._
import org.scanamo.{DynamoReadError, ScanamoCats, SecondaryIndex, Table}

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
  val table: Table[DynamoTwitterUser] = Table[DynamoTwitterUser]("dog-eared-main")
  val sortKeyData: SecondaryIndex[DynamoTwitterUser] = table.index("sortKeyData")

  def findByUniqueKeyOp(
      twitterUserId: TwitterUserId,
      shardSize: Shard.Size): ScanamoOps[Option[Either[DynamoReadError, DynamoTwitterUser]]] =
    table.get("primaryKey" -> primaryKey(twitterUserId) and "sortKey" -> sortKey(twitterUserId, shardSize))

  def findByShardOp(shard: Shard.Size): ScanamoOps[List[Either[DynamoReadError, DynamoTwitterUser]]] =
    sortKeyData.query("sortKey" -> sortKeyForShard(shard))

  def primaryKey(twitterUserId: TwitterUserId): String = s"TWITTER_USER#${twitterUserId}"
  def sortKey(twitterUserId: TwitterUserId, shardSize: Shard.Size): String =
    sortKeyForShard(Shard.determine(twitterUserId, shardSize))

  def sortKeyForShard(shardId: Shard.Size): String =
    s"TWITTER_USER#${shardId}"

  def from(twitterUser: TwitterUser, shardSize: Shard.Size): DynamoTwitterUser = DynamoTwitterUser(
    primaryKey = primaryKey(twitterUser.id),
    sortKey = sortKey(twitterUser.id, shardSize),
    data = twitterUser.username,
  )
}

case class DynamoTwitterUsers[F[_]: ContextShift: MonadError[*[_], Throwable]: Parallel](
    scanamo: ScanamoCats[F],
    logger: Logger[F],
    shardSize: Shard.Size)
    extends TwitterUsers[F]
    with TwitterUserQueries[F] {
  import DynamoTwitterUser._
  import DynamoErrorSyntax._

  override def resolve(id: TwitterUserId): F[Option[TwitterUser]] =
    for {
      _ <- logger.info(s"resolving twitter user ${id}")
      twitterUser <- scanamo.exec(findByUniqueKeyOp(id, shardSize)).raiseIfError.map(_.map(_.toTwitterUser))
      _ <- logger.info(twitterUser.toString)
    } yield twitterUser

  override def resolveAll: F[List[TwitterUser]] =
    for {
      _ <- logger.info(s"resolving all twitter users")
      twitterUsers <- (0 until shardSize).toList
        .parTraverse(shard => scanamo.exec(findByShardOp(Shard.size(shard))).raiseIfError.map(_.map(_.toTwitterUser)))
        .map(_.flatten)
    } yield twitterUsers

  override def store(twitterUser: TwitterUser): F[Unit] =
    logger.info(s"storing twitter user ${twitterUser.id} (@${twitterUser.username})") *>
      scanamo.exec(table.put(DynamoTwitterUser.from(twitterUser, shardSize)))

  override def storeMany(twitterUsers: List[TwitterUser]): F[Unit] =
    logger.info(s"storing ${twitterUsers.length} twitter users") *>
      scanamo.exec(table.putAll(twitterUsers.map(DynamoTwitterUser.from(_, shardSize)).toSet))
}
