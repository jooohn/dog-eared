package me.jooohn.dogeared.drivenadapters.dynamodb

import java.net.URL

import cats.{Monad, MonadError}
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.domain.{KindleBookId, KindleQuote, KindleQuotedTweet, TweetId, TwitterUserId}
import me.jooohn.dogeared.drivenports.{KindleQuotedTweetQueries, KindleQuotedTweets}
import org.scanamo.syntax._
import org.scanamo.generic.auto._
import org.scanamo.ops.ScanamoOps
import org.scanamo.{DynamoReadError, ScanamoCats, SecondaryIndex, Table}

import scala.util.Try

case class DynamoKindleQuotedTweet(
    primaryKey: String,
    sortKey: String,
    data: String,
    quoteBody: String,
    quoteUrl: String,
) {
  import DynamoKindleQuotedTweet._

  def twitterUserId: TwitterUserId = primaryKey.stripPrefix(primaryKeyPrefix)
  def tweetId: TweetId = sortKey.split("#")(2)
  def bookId: KindleBookId = sortKey.split("#")(1)

  def toKindleQuotedTweet: KindleQuotedTweet = KindleQuotedTweet(
    tweetId = tweetId,
    twitterUserId = twitterUserId,
    quote = KindleQuote(
      bookId = bookId,
      url = Try(new URL(quoteUrl)).getOrElse {
        throw new RuntimeException(s"url '${quoteUrl}' cannot be mapped to URL. (${this})")
      },
      body = quoteBody,
    )
  )

}
object DynamoKindleQuotedTweet {
  val table: Table[DynamoKindleQuotedTweet] = Table[DynamoKindleQuotedTweet]("dog-eared-main")
  val dataSortKey: SecondaryIndex[DynamoKindleQuotedTweet] = table.index("dataSortKey")
  val primaryKeyPrefix: String = "TWITTER_USER#"
  val sortKeyPrefix: String = "QUOTED_TWEET#"

  def findByTwitterUserIdOp(
      twitterUserId: TwitterUserId): ScanamoOps[List[Either[DynamoReadError, DynamoKindleQuotedTweet]]] =
    table.query("primaryKey" -> primaryKey(twitterUserId) and ("sortKey" beginsWith sortKeyPrefix))

  def findByKindleBookIdOp(
      kindleBookId: KindleBookId): ScanamoOps[List[Either[DynamoReadError, DynamoKindleQuotedTweet]]] =
    dataSortKey.query("data" -> data(kindleBookId) and ("sortKey" beginsWith sortKeyPrefix))

  def findByTwitterUserIdAndKindleBookIdOp(
      twitterUserId: TwitterUserId,
      kindleBookId: KindleBookId): ScanamoOps[List[Either[DynamoReadError, DynamoKindleQuotedTweet]]] =
    table.query(
      "primaryKey" -> primaryKey(twitterUserId) and ("sortKey" beginsWith s"${sortKeyPrefix}${kindleBookId}#"))

  def primaryKey(twitterUserId: TwitterUserId): String = s"${primaryKeyPrefix}${twitterUserId}"
  def data(kindleBookId: KindleBookId): String = s"BOOK#${kindleBookId}"

  def from(kindleQuotedTweet: KindleQuotedTweet): DynamoKindleQuotedTweet = DynamoKindleQuotedTweet(
    primaryKey = primaryKey(kindleQuotedTweet.twitterUserId),
    sortKey = s"${sortKeyPrefix}${kindleQuotedTweet.bookId}#${kindleQuotedTweet.tweetId}",
    data = data(kindleQuotedTweet.bookId),
    quoteBody = kindleQuotedTweet.quote.body,
    quoteUrl = kindleQuotedTweet.quote.url.toString,
  )

}

case class DynamoKindleQuotedTweets[F[_]: MonadError[*[_], Throwable]](scanamo: ScanamoCats[F], logger: Logger[F])
    extends KindleQuotedTweets[F]
    with KindleQuotedTweetQueries[F] {
  import DynamoKindleQuotedTweet._
  import DynamoErrorSyntax._

  override def storeMany(kindleQuotedTweets: List[KindleQuotedTweet]): F[Unit] =
    for {
      _ <- logger.info(s"storing ${kindleQuotedTweets.length} kindle quoted tweets.")
      _ <- scanamo.exec(table.putAll(kindleQuotedTweets.map(from).toSet))
    } yield ()

  override def resolveByUserId(userId: TwitterUserId): F[List[KindleQuotedTweet]] =
    scanamo.exec(findByTwitterUserIdOp(userId)).raiseIfError.map(_.map(_.toKindleQuotedTweet))

  override def resolveByBookId(bookId: KindleBookId): F[List[KindleQuotedTweet]] =
    scanamo.exec(findByKindleBookIdOp(bookId)).raiseIfError.map(_.map(_.toKindleQuotedTweet))

  override def resolveByUserIdAndBookId(userId: TwitterUserId, bookId: KindleBookId): F[List[KindleQuotedTweet]] =
    scanamo.exec(findByTwitterUserIdAndKindleBookIdOp(userId, bookId)).raiseIfError.map(_.map(_.toKindleQuotedTweet))
}
