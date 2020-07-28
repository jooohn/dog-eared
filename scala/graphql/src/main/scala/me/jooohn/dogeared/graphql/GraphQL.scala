package me.jooohn.dogeared.graphql

import caliban.GraphQL.graphQL
import caliban.schema.{GenericSchema, Schema}
import caliban.wrappers.Wrappers._
import caliban.{CalibanError, GraphQLInterpreter, RootResolver}
import cats.effect.ConcurrentEffect
import me.jooohn.dogeared.drivenports.Logger
import me.jooohn.dogeared.graphql.Wrappers._
import zio.console.Console
import zio.{IO, RIO}

object GraphQL {

  def apply[F[_]](
      resolvers: Resolvers,
      logger: Logger
  )(implicit CE: ConcurrentEffect[Effect]): IO[CalibanError.ValidationError, GraphQLInterpreter[Env, CalibanError]] = {
    object schema extends GenericSchema[Env]
    import schema._

    implicit lazy val idSchema: Schema[Env, Id] = Id.idSchema[Env]
    implicit lazy val quoteSchema: Schema[Env, Quote[Effect]] = gen

    val api = graphQL(RootResolver(resolvers.queries, resolvers.mutations)) @@
      errorLogging(logger) @@
      maxDepth(20) @@
      verifyInternalRequest
    api.interpreter
  }

}
