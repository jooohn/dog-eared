package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.Parallel
import cats.effect.{ContextShift, Sync}
import cats.implicits._
import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId, TwitterUsername}
import me.jooohn.dogeared.drivenports.{Context, TwitterUserQueries, TwitterUsers}
import org.scanamo.generic.auto._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._
import org.scanamo.{DynamoReadError, SecondaryIndex, Table}

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
  val dataSortKey: SecondaryIndex[DynamoTwitterUser] = table.index("dataSortKey")

  def findByUniqueKeyOp(
      twitterUserId: TwitterUserId,
      shardSize: Shard.Size): ScanamoOps[Option[Either[DynamoReadError, DynamoTwitterUser]]] =
    table.get("primaryKey" -> primaryKey(twitterUserId) and "sortKey" -> sortKey(twitterUserId, shardSize))

  def findByShardOp(shard: Shard.Size): ScanamoOps[List[Either[DynamoReadError, DynamoTwitterUser]]] =
    sortKeyData.query("sortKey" -> sortKeyForShard(shard))

  def findByUsernameOp(
      twitterUsername: TwitterUsername): ScanamoOps[Option[Either[DynamoReadError, DynamoTwitterUser]]] =
    dataSortKey.query(("data" -> twitterUsername) and ("sortKey" beginsWith "TWITTER_USER#")).map(_.headOption)

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

case class DynamoTwitterUsers[F[_]: ContextShift: Sync: Parallel](scanamo: TracingScanamo[F], shardSize: Shard.Size)
    extends TwitterUsers[F]
    with TwitterUserQueries[F] {
  import DynamoErrorSyntax._
  import DynamoTwitterUser._

  override def resolve(id: TwitterUserId)(implicit ctx: Context[F]): F[Option[TwitterUser]] =
    for {
      _ <- ctx.logger.info(s"resolving twitter user ${id}")
      twitterUser <- scanamo.exec(findByUniqueKeyOp(id, shardSize)).raiseIfError.map(_.map(_.toTwitterUser))
      _ <- ctx.logger.info(twitterUser.toString)
    } yield twitterUser

  override def resolveByUsername(username: TwitterUsername)(implicit ctx: Context[F]): F[Option[TwitterUser]] =
    for {
      _ <- ctx.logger.info(s"resolving twitter user by username ${username}")
      twitterUser <- scanamo.exec(findByUsernameOp(username)).raiseIfError.map(_.map(_.toTwitterUser))
      _ <- ctx.logger.info(twitterUser.toString)
    } yield twitterUser

  override def resolveAll(implicit ctx: Context[F]): F[List[TwitterUser]] =
    for {
      _ <- ctx.logger.info(s"resolving all twitter users")
      twitterUsers <- (0 until shardSize).toList
        .parTraverse(shard => scanamo.exec(findByShardOp(Shard.size(shard))).raiseIfError.map(_.map(_.toTwitterUser)))
        .map(_.flatten)
    } yield twitterUsers

  override def store(twitterUser: TwitterUser)(implicit ctx: Context[F]): F[Unit] =
    ctx.logger.info(s"storing twitter user ${twitterUser.id} (@${twitterUser.username})") *>
      scanamo.exec(table.put(DynamoTwitterUser.from(twitterUser, shardSize)))

  override def storeMany(twitterUsers: List[TwitterUser])(implicit ctx: Context[F]): F[Unit] =
    ctx.logger.info(s"storing ${twitterUsers.length} twitter users") *>
      scanamo.exec(table.putAll(twitterUsers.map(DynamoTwitterUser.from(_, shardSize)).toSet))
}
