package me.jooohn.dogeared.graphql

import caliban.GraphQL.graphQL
import caliban.interop.cats.CatsInterop
import caliban.interop.cats.implicits._
import caliban.schema.Schema
import caliban.wrappers.Wrappers._
import caliban.{CalibanError, GraphQLInterpreter, RootResolver}
import cats.effect.{ConcurrentEffect, IO}
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.graphql.Wrappers._
import zio.Runtime

object GraphQL {

  def interpreter(resolvers: Resolvers, logger: Logger[IO])(
      implicit CE: ConcurrentEffect[IO],
      R: Runtime[zio.ZEnv]): IO[GraphQLInterpreter[zio.ZEnv, CalibanError]] = {
    implicit lazy val quoteSchema: Schema[Any, Quote] = Schema.gen
    val api = graphQL(RootResolver(resolvers.queries, resolvers.mutations)) @@
      errorLogging(logger) @@
      maxDepth(20) @@
      verifyInternalRequest
    CatsInterop.interpreterAsync(api)
  }

}
