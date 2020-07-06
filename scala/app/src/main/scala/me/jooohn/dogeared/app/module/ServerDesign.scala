package me.jooohn.dogeared.app.module

import cats.effect.Resource
import me.jooohn.dogeared.app.ServerConfig
import me.jooohn.dogeared.drivenports.Logger
import me.jooohn.dogeared.graphql.{GraphQL, Resolvers}
import me.jooohn.dogeared.server.HttpService
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.Future

trait ServerDesign { self: DSLBase with AdapterDesign with ConfigDesign =>
  implicit def server: Bind[Server[Effect]] =
    singleton(for {
      resolvers <- inject[Resolvers[Env]]
      logger <- inject[Logger]
      config <- inject[ServerConfig]
      builder <- injectF(Resource.liftF[Effect, BlazeServerBuilder[Effect]](for {
        interpreter <- GraphQL(resolvers, logger)
        builder <- Effect.fromFuture(ec => Future.successful(BlazeServerBuilder[Effect](ec)))
      } yield builder.bindHttp(config.port, "0.0.0.0").withHttpApp(HttpService[Env](interpreter, logger).routes)))
      server <- injectF(builder.resource)
    } yield server)
}
