package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}

trait TwitterUsers[F[_]] {

  def resolve(id: TwitterUserId): F[Option[TwitterUser]]

  def resolveAll: F[List[TwitterUser]]

  def store(twitterUser: TwitterUser): F[Unit]

  def storeMany(twitterUser: List[TwitterUser]): F[Unit]

}
