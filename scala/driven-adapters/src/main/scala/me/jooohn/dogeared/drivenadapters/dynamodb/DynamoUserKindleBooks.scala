package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.Monad
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.domain.{KindleBookId, TwitterUserId}
import me.jooohn.dogeared.drivenports.{UserKindleBook, UserKindleBooks}
import org.scanamo.generic.auto._
import org.scanamo.ops.ScanamoOps
import org.scanamo.syntax._
import org.scanamo.{DynamoReadError, ScanamoCats, Table}

case class DynamoUserKindleBook(
    primaryKey: String,
    sortKey: String,
    data: String,
) {

  def kindleBookId: KindleBookId = sortKey.stripPrefix(DynamoUserKindleBook.sortKeyPrefix)

}
object DynamoUserKindleBook {

  def primaryKey(twitterUserId: TwitterUserId): String = s"TWITTER_USER#${twitterUserId}"
  def sortKey(kindleBookId: KindleBookId): String = s"${sortKeyPrefix}${kindleBookId}"

  def from(userKindleBook: UserKindleBook): DynamoUserKindleBook = DynamoUserKindleBook(
    primaryKey = primaryKey(userKindleBook.twitterUserId),
    sortKey = sortKey(userKindleBook.kindleBookId),
    data = userKindleBook.twitterUserId
  )

  val table: Table[DynamoUserKindleBook] = Table[DynamoUserKindleBook]("dog-eared-main")
  val sortKeyPrefix: String = "TWITTER_USER_KINDLE_BOOK#"

  def queryByUserIdOp(twitterUserId: TwitterUserId): ScanamoOps[List[Either[DynamoReadError, DynamoUserKindleBook]]] =
    table.query("primaryKey" -> primaryKey(twitterUserId) and ("sortKey" beginsWith sortKeyPrefix))
}
case class DynamoUserKindleBooks[F[_]: Monad](scanamo: ScanamoCats[F], logger: Logger[F]) extends UserKindleBooks[F] {
  import DynamoUserKindleBook._

  override def storeMany(userKindleBooks: List[UserKindleBook]): F[Unit] =
    for {
      _ <- scanamo.exec(table.putAll(userKindleBooks.toSet.map(DynamoUserKindleBook.from)))
    } yield ()

}
