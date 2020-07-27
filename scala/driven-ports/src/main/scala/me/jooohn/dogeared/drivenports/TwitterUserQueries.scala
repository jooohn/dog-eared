package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId, TwitterUsername}

trait TwitterUserQueries[F[_]] {

  def resolve(twitterUserId: TwitterUserId)(implicit ctx: Context[F]): F[Option[TwitterUser]]
  def resolveAll(implicit ctx: Context[F]): F[List[TwitterUser]]
  def resolveByUsername(username: TwitterUsername)(implicit ctx: Context[F]): F[Option[TwitterUser]]

}
