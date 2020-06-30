package me.jooohn.dogeared.graphql

import caliban.GraphQL.graphQL
import caliban.interop.cats.CatsInterop
import caliban.interop.cats.implicits._
import caliban.schema.Schema
import caliban.wrappers.Wrappers._
import caliban.{CalibanError, GraphQLInterpreter, RootResolver}
import cats.effect.{ConcurrentEffect, IO}
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}
import me.jooohn.dogeared.graphql.Wrappers._
import zio.Runtime

object GraphQL {

  def interpreter(
      twitterUserQueries: TwitterUserQueries[IO],
      kindleQuotedTweetQueries: KindleQuotedTweetQueries[IO],
      kindleBookQueries: KindleBookQueries[IO],
      logger: Logger[IO],
  )(implicit CE: ConcurrentEffect[IO], R: Runtime[zio.ZEnv]): IO[GraphQLInterpreter[zio.ZEnv, CalibanError]] = {
    implicit lazy val quoteSchema: Schema[Any, Quote] = Schema.gen

    val resolvers = new Resolvers(
      twitterUserQueries = twitterUserQueries,
      kindleBookQueries = kindleBookQueries,
      kindleQuotedTweetQueries = kindleQuotedTweetQueries,
    )

    val api = graphQL(RootResolver(resolvers.queries)) @@
      errorLogging(logger) @@
      maxDepth(20)
    CatsInterop.interpreterAsync(api)
  }

}
