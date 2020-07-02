package me.jooohn.dogeared.app.module

import caliban.{CalibanError, GraphQLInterpreter}
import cats.effect.{IO, Resource}
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.app.ServerConfig
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}
import me.jooohn.dogeared.graphql.{GraphQL, Resolvers}
import me.jooohn.dogeared.server.HttpService
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import caliban.interop.cats.implicits._

trait ServerDesign { self: DSLBase with AdapterDesign with ConfigDesign =>
  private type GraphQL = GraphQLInterpreter[zio.ZEnv, CalibanError]

  implicit def graphQL: Bind[GraphQL] =
    singleton(for {
      resolvers <- inject[Resolvers]
      logger <- inject[Logger[IO]]
      interpreter <- injectF[GraphQL](Resource.liftF[IO, GraphQL](GraphQL.interpreter(resolvers, logger)))
    } yield interpreter)

  implicit def server: Bind[Server[IO]] =
    singleton(for {
      config <- inject[ServerConfig]
      interpreter <- inject[GraphQL]
      server <- injectF(
        BlazeServerBuilder[IO](scala.concurrent.ExecutionContext.global)
          .bindHttp(config.port, "0.0.0.0")
          .withHttpApp(HttpService(interpreter))
          .resource)
    } yield server)
}
