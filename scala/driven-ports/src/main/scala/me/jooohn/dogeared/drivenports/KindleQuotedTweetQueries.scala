package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{KindleBookId, KindleQuotedTweet, TwitterUserId}

trait KindleQuotedTweetQueries[F[_]] {

  def resolveByUserId(userId: TwitterUserId): F[List[KindleQuotedTweet]]

  def resolveByBookId(bookId: KindleBookId): F[List[KindleQuotedTweet]]

  def resolveByUserIdAndBookId(userId: TwitterUserId, bookId: KindleBookId): F[List[KindleQuotedTweet]]

}
