package me.jooohn.dogeared.server

import ciris._
import cats.implicits._
import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import ciris.ConfigValue
import org.http4s.Uri
import org.http4s.client.blaze.BlazeClientBuilder

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource.use { client =>
      for {
        config <- RuntimeConfig.configValue.load[IO]
        exitCode <- {
          val runtimeAPI = new LambdaRuntimeAPI(config.runtimeAPI, client)
          new LambdaRuntime(runtimeAPI, config.handler).run
        }
      } yield exitCode
    }

  val loadRuntimeAPIBaseUri: IO[Uri] =
    for {
      uriString <- IO.fromOption(sys.env.get("AWS_LAMBDA_RUNTIME_API"))(
        new RuntimeException("AWS_LAMBDA_RUNTIME_API environment variable should be set"))
      uri <- IO.fromEither(Uri.fromString(uriString))
    } yield uri

  val loadHandler: IO[Handler] =
    for {
      handlerName <- IO.fromOption(sys.env.get("_HANDLER"))(
        new RuntimeException("_HANDLER environment variable should be set"))
      handler <- IO.fromOption(Handlers(handlerName))(
        new RuntimeException(s"""handler "${handlerName}" is not found"""))
    } yield handler

}

case class RuntimeConfig(
    runtimeAPI: Uri,
    handler: Handler
)
object RuntimeConfig {

  def configValue: ConfigValue[RuntimeConfig] =
    (
      env("AWS_LAMBDA_RUNTIME_API").map(hostAndPort => Uri.fromString(s"http://${hostAndPort}")).flatMap {
        case Left(e)    => ConfigValue.failed[Uri](ConfigError(e.message))
        case Right(uri) => ConfigValue.default(uri)
      },
      env("_HANDLER") flatMap { handler =>
        Handlers(handler).fold[ConfigValue[Handler]](
          ConfigValue.failed(ConfigError(s"${handler} is not a valid handler")))(h => ConfigValue.default(h))
      }
    ).parMapN(RuntimeConfig.apply)

}
