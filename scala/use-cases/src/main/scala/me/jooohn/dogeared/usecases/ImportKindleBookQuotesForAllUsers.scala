package me.jooohn.dogeared.usecases

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import me.jooohn.dogeared.drivenports.TwitterUsers

class ImportKindleBookQuotesForAllUsers[F[_]: Monad](
    importKindleBookQuotesForUser: ImportKindleBookQuotesForUser[F],
    twitterUsers: TwitterUsers[F]
) {
  import ImportKindleBookQuotesForAllUsers._

  def apply: F[Either[Error, Unit]] =
    for {
      users <- twitterUsers.resolveAll
      results <- users.traverse(user => importKindleBookQuotesForUser(user.id))
    } yield gatherResults(results)

  private def gatherResults(results: List[Either[ImportKindleBookQuotesForUser.Error, Unit]]): Either[Error, Unit] =
    results
      .traverse(_.toValidatedNel)
      .bimap(
        Errors.apply,
        _ => ()
      )
      .toEither
}
object ImportKindleBookQuotesForAllUsers {
  sealed trait Error
  case class Errors(errors: NonEmptyList[ImportKindleBookQuotesForUser.Error]) extends Error
}
