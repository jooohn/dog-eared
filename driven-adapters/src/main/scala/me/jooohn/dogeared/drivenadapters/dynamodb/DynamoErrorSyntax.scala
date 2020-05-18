package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.Traverse
import cats.effect.IO
import cats.implicits._
import org.scanamo.DynamoReadError

object DynamoErrorSyntax {

  implicit class DynamoReadErrorOrOps[A](errorOr: Either[DynamoReadError, A]) {

    def toIO: IO[A] = errorOr.fold(
      error => IO.raiseError(new RuntimeException(DynamoReadError.describe(error))),
      IO.pure
    )

  }

  implicit class DynamoResultOps[F[_]: Traverse, A](result: IO[F[Either[DynamoReadError, A]]]) {

    def raiseIfError: IO[F[A]] = result.map(_.sequence).flatMap(_.toIO)

  }

}
