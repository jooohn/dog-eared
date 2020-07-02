package me.jooohn.dogeared.graphql

import caliban.CalibanError.ExecutionError
import caliban.execution.{ExecutionRequest, Field}
import caliban.wrappers.Wrapper.{ExecutionWrapper, OverallWrapper, ValidationWrapper}
import cats.effect.ConcurrentEffect
import io.chrisdavenport.log4cats.Logger
import zio.{URIO, ZIO}

object Wrappers {

  def errorLogging[F[_]: ConcurrentEffect](logger: Logger[F]): ExecutionWrapper[Any] =
    ExecutionWrapper { process => request =>
      for {
        response <- process(request)
        _ <- ZIO.collectAllPar(response.errors.collect {
          case ExecutionError(_, _, _, Some(throwable), _) =>
            // TODO
            ConcurrentEffect[F].toIO(logger.error(throwable)(throwable.getMessage)).unsafeRunAsyncAndForget()
            URIO(throwable)
        })
      } yield response
    }

  def verifyInternalRequest[F[_]: ConcurrentEffect]: ExecutionWrapper[Any] =
    ExecutionWrapper { process => request =>
      if (request.includesInternalFields) {
        println("Includes internal fields")
        process(request)
      } else {
        process(request)
      }
    }

  implicit class RequestOps(request: ExecutionRequest) {

    def includesInternalFields: Boolean = request.field.includesInternalFields

  }

  implicit class FieldOps(field: Field) {

    def includesInternalFields: Boolean =
      field.directives.contains(Directives.internal) || field.fields.exists(_.includesInternalFields)
  }
}
