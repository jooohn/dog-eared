package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.implicits._
import cats.{Monad, MonadError, Traverse}
import org.scanamo.DynamoReadError

object DynamoErrorSyntax {

  implicit class DynamoReadErrorOrOps[A](errorOr: Either[DynamoReadError, A]) {

    def toF[F[_]: MonadError[*[_], Throwable]]: F[A] = errorOr.fold(
      error => MonadError[F, Throwable].raiseError[A](new RuntimeException(DynamoReadError.describe(error))),
      Monad[F].pure
    )

  }

  implicit class DynamoResultOps[F[_]: MonadError[*[_], Throwable], G[_]: Traverse, A](
      result: F[G[Either[DynamoReadError, A]]]) {

    def raiseIfError: F[G[A]] = result.map(_.sequence).flatMap(_.toF[F])

  }

}
