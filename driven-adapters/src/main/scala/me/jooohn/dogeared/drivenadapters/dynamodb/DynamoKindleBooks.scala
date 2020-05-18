package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.IO
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import me.jooohn.dogeared.domain.KindleBook
import me.jooohn.dogeared.drivenports.KindleBooks
import org.scanamo._
import org.scanamo.generic.auto._

case class DynamoKindleBook(
    primaryKey: String,
    sortKey: String,
    data: String,
    bookId: String,
    url: String,
    authors: Set[String]
)
object DynamoKindleBook {
  def from(kindleBook: KindleBook, shardSize: Int): DynamoKindleBook = DynamoKindleBook(
    primaryKey = s"KINDLE_BOOK#${kindleBook.id}",
    sortKey = s"KINDLE_BOOK#${Shard.determine(kindleBook.id, shardSize)}",
    data = kindleBook.title,
    bookId = kindleBook.id,
    url = kindleBook.url.toString,
    authors = kindleBook.authors.toSet,
  )
}

class DynamoKindleBooks(scanamo: ScanamoCats[IO], logger: Logger[IO], shardSize: Int) extends KindleBooks[IO] {
  val table: Table[DynamoKindleBook] = Table("dog-eared-books")

  override def storeMany(kindleBooks: List[KindleBook]): IO[Unit] =
    for {
      _ <- logger.info(s"storing ${kindleBooks.length} kindle books")
      _ <- scanamo.exec(table.putAll(kindleBooks.map(DynamoKindleBook.from(_, shardSize = shardSize)).toSet))
    } yield ()

}
