package me.jooohn.dogeared.drivenadapters

import cats.Monad
import cats.effect.Bracket
import cats.implicits._
import doobie.implicits._
import doobie.{Transactor, Update}
import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}
import me.jooohn.dogeared.drivenadapters.instances.twitterUser._
import me.jooohn.dogeared.drivenports.TwitterUsers

class PGTwitterUsers[F[_]: Monad: Transactor: Bracket[*[_], Throwable]] extends TwitterUsers[F] {

  val transactor: Transactor[F] = implicitly[Transactor[F]]

  override def storeMany(twitterUsers: List[TwitterUser]): F[Unit] = {
    val sql =
      """
        |INSERT INTO twitter_users
        |(id, username)
        |VALUES (?, ?) ON CONFLICT (id) DO UPDATE
        |SET username = EXCLUDED.username
        |""".stripMargin
    Update[TwitterUser](sql)
      .updateMany(twitterUsers)
      .transact[F](implicitly[Transactor[F]]) *> Monad[F].unit
  }

  override def resolve(id: TwitterUserId): F[Option[TwitterUser]] =
    sql"""
         |SELECT id, username
         |FROM twitter_users
         |WHERE id = ${id}
         |""".stripMargin
      .query[TwitterUser]
      .option
      .transact(transactor)

  override def store(twitterUser: TwitterUser): F[Unit] =
    storeMany(List(twitterUser))

  override def resolveAll: F[List[TwitterUser]] =
    sql"""
         |SELECT id, username
         |FROM twitter_users
         |""".stripMargin
      .query[TwitterUser]
      .to[List]
      .transact(transactor)
}
