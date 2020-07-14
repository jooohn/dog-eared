package me.jooohn.dogeared.usecases

import cats.Monad
import cats.data.{EitherT, OptionT}
import me.jooohn.dogeared.domain.{TwitterUser, TwitterUserId}
import me.jooohn.dogeared.drivenports.{Twitter, TwitterUsers}

case class ImportUser[F[_]: Monad](
    twitter: Twitter[F],
    twitterUsers: TwitterUsers[F]
) {
  import ImportUser._

  def apply(identity: String): F[Either[Error, TwitterUserId]] =
    (for {
      twitterUser <- findByIdOrName(identity)
      _ <- EitherT.liftF[F, Error, Unit](twitterUsers.store(twitterUser))
    } yield twitterUser.id).value

  private def findByIdOrName(identity: String): EitherT[F, Error, TwitterUser] =
    OptionT
      .fromOption(identity.toLongOption)
      .flatMapF(id => twitter.findUserAccount(id.toString))
      .orElseF(twitter.findUserAccountByName(identity))
      .toRight(UserNotFound(identity))

}

object ImportUser {
  sealed trait Error
  case class UserNotFound(identity: String) extends Error
}
