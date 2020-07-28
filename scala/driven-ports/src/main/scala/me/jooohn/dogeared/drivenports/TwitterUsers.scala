package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}

trait TwitterUsers[F[_]] {

  def resolve(id: TwitterUserId)(implicit ctx: Context[F]): F[Option[TwitterUser]]

  def resolveAll(implicit ctx: Context[F]): F[List[TwitterUser]]

  def store(twitterUser: TwitterUser)(implicit ctx: Context[F]): F[Unit]

  def storeMany(twitterUsers: List[TwitterUser])(implicit ctx: Context[F]): F[Unit]

}
