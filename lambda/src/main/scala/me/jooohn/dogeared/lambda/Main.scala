package me.jooohn.dogeared.lambda

import cats.effect.{ExitCode, IO, IOApp, Resource}
import me.jooohn.dogeared.app.ProductionApp
import me.jooohn.dogeared.lambda.LambdaRuntimeApi.InvocationFailure
import org.http4s.client.blaze.BlazeClientBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    for {
      config <- RuntimeConfig.configValue.load[IO]
      exitCode <- lambdaRuntimeApiResource(config).use { runtimeApi =>
        ProductionApp { app =>
          config.handlerFactory(app) flatMap { handler =>
            new LambdaRuntime(runtimeApi, handler).run
          }
        } handleErrorWith { throwable =>
          IO(println(throwable)) *>
            runtimeApi.initializationError(
              InvocationFailure(
                errorType = "INITIALIZATION_ERROR",
                errorMessage = throwable.getMessage
              )) *>
            IO.pure(ExitCode.Error)
        }
      }
    } yield exitCode

  def lambdaRuntimeApiResource(config: RuntimeConfig): Resource[IO, LambdaRuntimeApi] =
    BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource.map { client =>
      new LambdaRuntimeApi(config.runtimeAPI, client)
    }
}
