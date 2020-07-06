package me.jooohn.dogeared.graphql

import caliban.CalibanError.ExecutionError
import caliban.execution.{ExecutionRequest, Field}
import caliban.wrappers.Wrapper.ExecutionWrapper
import caliban.{CalibanError, GraphQLResponse, Value}
import cats.effect.ConcurrentEffect
import me.jooohn.dogeared.drivenports.Logger
import me.jooohn.dogeared.graphql.GraphQLContextRepository._
import zio.console._
import zio.interop.catz._
import zio.{Task, ZIO}

object Wrappers {

  def errorLogging[F[_]: ConcurrentEffect](logger: Logger): ExecutionWrapper[Console] =
    ExecutionWrapper { process => request =>
      for {
        response <- process(request)
        _ <- ZIO.collectAllPar(response.errors.collect {
          case ExecutionError(_, _, _, Some(throwable), _) =>
            Task.concurrentEffectWith { implicit CE =>
              CE.liftIO(ConcurrentEffect[F].toIO(logger.error[F](throwable)))
            } catchAll { t =>
              putStrLn(t.toString)
            }
        })
      } yield response
    }

  def verifyInternalRequest: ExecutionWrapper[GraphQLContextRepository] =
    ExecutionWrapper { process => request =>
      if (request.includesInternalField) {
        for {
          context <- getGraphQLContext
          response <- if (context.isInternalRequest) process(request)
          else
            ZIO.succeed(
              GraphQLResponse(
                data = Value.NullValue,
                errors = List(
                  CalibanError.ValidationError(
                    msg = "Unauthorized",
                    explanatoryText = "You are not allowed to execute this query.",
                  ))
              ))
        } yield response
      } else {
        process(request)
      }
    }

  implicit class RequestOps(request: ExecutionRequest) {

    def includesInternalField: Boolean = request.field.includesInternalField

  }

  implicit class FieldOps(field: Field) {

    def includesInternalField: Boolean =
      field.directives.contains(Directives.internal) || field.fields.exists(_.includesInternalField)
  }
}
