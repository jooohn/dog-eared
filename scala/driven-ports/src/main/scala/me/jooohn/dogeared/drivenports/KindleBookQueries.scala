package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{KindleBook, KindleBookId, TwitterUserId}

trait KindleBookQueries[F[_]] {

  def resolve(bookId: KindleBookId): F[Option[KindleBook]]

  def resolveByUserId(userId: TwitterUserId): F[List[KindleBook]]

}
