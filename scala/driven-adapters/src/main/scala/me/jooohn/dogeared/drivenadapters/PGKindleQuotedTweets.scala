package me.jooohn.dogeared.drivenadapters

import cats.Monad
import cats.effect.Bracket
import cats.implicits._
import doobie._
import doobie.implicits._
import me.jooohn.dogeared.domain.KindleQuotedTweet
import me.jooohn.dogeared.drivenadapters.instances.kindleQuotedTweet._
import me.jooohn.dogeared.drivenports.KindleQuotedTweets

class PGKindleQuotedTweets[F[_]: Monad: Transactor: Bracket[*[_], Throwable]] extends KindleQuotedTweets[F] {

  override def storeMany(kindleQuotedTweets: List[KindleQuotedTweet]): F[Unit] = {
    val sql =
      """
        |INSERT INTO kindle_quoted_tweets
        |(tweet_id, twitter_user_id, book_id, quote_url, quote_body)
        |VALUES (?, ?, ?, ?, ?) ON CONFLICT (tweet_id) DO UPDATE
        |SET book_id    = EXCLUDED.book_id,
        |    quote_url  = EXCLUDED.quote_url,
        |    quote_body = EXCLUDED.quote_body
        |""".stripMargin
    Update[KindleQuotedTweet](sql)
      .updateMany(kindleQuotedTweets)
      .transact[F](implicitly[Transactor[F]]) *> Monad[F].unit
  }

}
