package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.IO
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.domain.KindleQuotedTweet
import me.jooohn.dogeared.drivenports.KindleQuotedTweets
import org.scanamo.generic.auto._
import org.scanamo.{ScanamoCats, Table}

case class DynamoKindleQuotedTweet(
    primaryKey: String,
    sortKey: String,
    data: String,
    quoteBody: String,
    quoteUrl: String,
)
object DynamoKindleQuotedTweet {

  def from(kindleQuotedTweet: KindleQuotedTweet): DynamoKindleQuotedTweet = DynamoKindleQuotedTweet(
    primaryKey = s"TWITTER_USER#${kindleQuotedTweet.twitterUserId}",
    sortKey = s"QUOTED_TWEET#${kindleQuotedTweet.bookId}#${kindleQuotedTweet.tweetId}",
    data = s"BOOK#${kindleQuotedTweet.bookId}",
    quoteBody = kindleQuotedTweet.quote.body,
    quoteUrl = kindleQuotedTweet.quote.url.toString,
  )

}

class DynamoKindleQuotedTweets(scanamo: ScanamoCats[IO], logger: Logger[IO]) extends KindleQuotedTweets[IO] {
  val table: Table[DynamoKindleQuotedTweet] = Table[DynamoKindleQuotedTweet]("dog-eared-users")

  override def storeMany(kindleQuotedTweets: List[KindleQuotedTweet]): IO[Unit] =
    for {
      _ <- logger.info(s"storing ${kindleQuotedTweets.length} kindle quoted tweets.")
      _ <- scanamo.exec(table.putAll(kindleQuotedTweets.map(DynamoKindleQuotedTweet.from).toSet))
    } yield ()
}
