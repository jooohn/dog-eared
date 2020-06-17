package me.jooohn.dogeared.lambda

import cats.effect.IO
import ciris.{ConfigError, ConfigValue, env}
import me.jooohn.dogeared.app.ProductionAppModule
import org.http4s.Uri
import cats.implicits._
import me.jooohn.dogeared.lambda.handler.Handlers

case class RuntimeConfig(
    runtimeAPI: Uri,
    handlerFactory: HandlerFactory,
)
object RuntimeConfig {

  def configValue: ConfigValue[RuntimeConfig] =
    (
      env("AWS_LAMBDA_RUNTIME_API").map(hostAndPort => Uri.fromString(s"http://${hostAndPort}")).flatMap {
        case Left(e)    => ConfigValue.failed[Uri](ConfigError(e.message))
        case Right(uri) => ConfigValue.default(uri)
      },
      env("_HANDLER") flatMap { handler =>
        Handlers
          .factory(handler)
          .fold[ConfigValue[ProductionAppModule => IO[Handler]]](
            ConfigValue.failed(ConfigError(s"${handler} is not a valid handler")))(h => ConfigValue.default(h))
      }
    ).parMapN(RuntimeConfig.apply)

}
