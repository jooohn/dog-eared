package me.jooohn.dogeared.drivenadapters.dynamodb

import java.net.URL

import cats.MonadError
import cats.effect.Sync
import cats.implicits._
import me.jooohn.dogeared.domain.{KindleBook, KindleBookId, TwitterUserId}
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleBooks, Logger}
import org.scanamo._
import org.scanamo.syntax._
import org.scanamo.generic.auto._

import scala.util.Try

case class DynamoKindleBook(
    primaryKey: String,
    sortKey: String,
    data: String,
    bookId: String,
    url: String,
    authors: Set[String]
) {

  def toKindleBook: KindleBook = KindleBook(
    id = bookId,
    title = data,
    url = Try(new URL(url)).getOrElse {
      throw new RuntimeException(s"url '${url}' cannot be mapped to URL. (${this})")
    },
    authors = authors.toList
  )

}
object DynamoKindleBook {

  val table: Table[DynamoKindleBook] = Table("dog-eared-main")
  val primaryKeyPrefix: String = "KINDLE_BOOK#"
  def primaryKey(kindleBookId: KindleBookId): String = s"${primaryKeyPrefix}${kindleBookId}"
  def sortKey(kindleBookId: KindleBookId, shardSize: Shard.Size): String =
    s"KINDLE_BOOK#${Shard.determine(kindleBookId, shardSize)}"

  def findById(id: KindleBookId, shardSize: Shard.Size) =
    table.get("primaryKey" -> primaryKey(id) and "sortKey" -> sortKey(id, shardSize))

  def from(kindleBook: KindleBook, shardSize: Shard.Size): DynamoKindleBook = DynamoKindleBook(
    primaryKey = primaryKey(kindleBook.id),
    sortKey = sortKey(kindleBook.id, shardSize),
    data = kindleBook.title,
    bookId = kindleBook.id,
    url = kindleBook.url.toString,
    authors = kindleBook.authors.toSet,
  )
}

case class DynamoKindleBooks[F[_]: Sync](scanamo: ScanamoCats[F], logger: Logger, shardSize: Shard.Size)
    extends KindleBooks[F]
    with KindleBookQueries[F] {
  import DynamoErrorSyntax._
  import DynamoKindleBook._

  override def storeMany(kindleBooks: List[KindleBook]): F[Unit] =
    for {
      _ <- logger.info(s"storing ${kindleBooks.length} kindle books")
      _ <- scanamo.exec(table.putAll(kindleBooks.map(DynamoKindleBook.from(_, shardSize = shardSize)).toSet))
    } yield ()

  override def resolve(bookId: KindleBookId): F[Option[KindleBook]] =
    for {
      kindleBook <- scanamo.exec(findById(bookId, shardSize)).raiseIfError
    } yield kindleBook.map(_.toKindleBook)

  override def resolveByUserId(userId: TwitterUserId): F[List[KindleBook]] = {
    for {
      userKindleBooks <- scanamo.exec(DynamoUserKindleBook.queryByUserIdOp(userId)).raiseIfError
      primaryKeys = userKindleBooks
        .map(userKindleBook =>
          (primaryKey(userKindleBook.kindleBookId), sortKey(userKindleBook.kindleBookId, shardSize)))
        .toSet
      kindleBooks <- scanamo.exec(table.getAll(("primaryKey" and "sortKey") -> primaryKeys)).map(_.toList).raiseIfError
    } yield kindleBooks.map(_.toKindleBook)
  }

}
