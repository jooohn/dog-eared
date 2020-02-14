package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.TwitterUser

trait TwitterUsers[F[_]] {

  def storeMany(twitterUser: List[TwitterUser]): F[Unit]

}
