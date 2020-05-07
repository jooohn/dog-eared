package me.jooohn.dogeared.usecases

import cats.Monad
import cats.implicits._
import cats.data.NonEmptyList
import me.jooohn.dogeared.drivenports.{ConcurrentIO, TwitterUsers}

class ImportKindleBookQuotesForAllUsers[F[_]: Monad](
    twitterUsers: TwitterUsers[F],
    importKindleBookQuotesForUser: ImportKindleBookQuotesForUser[F],
    concurrentIO: ConcurrentIO[F]
) {
  import ImportKindleBookQuotesForAllUsers._

  def apply: F[Either[Error, Unit]] =
    for {
      users <- twitterUsers.resolveAll
      results <- concurrentIO.all(users)(user => importKindleBookQuotesForUser(user.id))
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
