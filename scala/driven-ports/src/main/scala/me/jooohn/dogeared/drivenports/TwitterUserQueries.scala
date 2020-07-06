package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId, TwitterUsername}

trait TwitterUserQueries[F[_]] {

  def resolve(twitterUserId: TwitterUserId): F[Option[TwitterUser]]
  def resolveByUsername(username: TwitterUsername): F[Option[TwitterUser]]

}
