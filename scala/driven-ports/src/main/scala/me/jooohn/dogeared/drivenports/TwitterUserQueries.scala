package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}

trait TwitterUserQueries[F[_]] {

  def resolve(twitterUserId: TwitterUserId): F[Option[TwitterUser]]

}
