package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{KindleBookId, TwitterUserId}

case class UserKindleBook(
    twitterUserId: TwitterUserId,
    kindleBookId: KindleBookId,
)

trait UserKindleBooks[F[_]] {

  def storeMany(userKindleBooks: List[UserKindleBook]): F[Unit]

}
