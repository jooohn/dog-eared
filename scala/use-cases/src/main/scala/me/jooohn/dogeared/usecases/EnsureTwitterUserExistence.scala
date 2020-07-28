package me.jooohn.dogeared.usecases

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import me.jooohn.dogeared.domain.TwitterUserId
import me.jooohn.dogeared.drivenports.{Context, Twitter, TwitterUsers}

class EnsureTwitterUserExistence[F[_]: Monad](
    twitter: Twitter[F],
    twitterUsers: TwitterUsers[F]
) {
  import EnsureTwitterUserExistence._

  def apply(twitterUserId: TwitterUserId)(implicit ctx: Context[F]): F[Either[Error, Unit]] =
    for {
      twitterUser <- twitterUsers.resolve(twitterUserId)
      result <- twitterUser.fold(importTwitterUser(twitterUserId))(_ => Monad[F].pure(Right(())))
    } yield result

  private def importTwitterUser(twitterUserId: TwitterUserId)(implicit ctx: Context[F]): F[Either[Error, Unit]] =
    (for {
      twitterUser <- EitherT.fromOptionF(twitter.findUserAccount(twitterUserId), UserNotFound(twitterUserId): Error)
      _ <- EitherT.right[Error](twitterUsers.store(twitterUser))
    } yield ()).value

}

object EnsureTwitterUserExistence {
  sealed trait Error
  case class UserNotFound(id: TwitterUserId) extends Error
}
