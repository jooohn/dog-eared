package me.jooohn.dogeared.graphql

import caliban.CalibanError.ExecutionError
import caliban.wrappers.Wrapper.ExecutionWrapper
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

}
