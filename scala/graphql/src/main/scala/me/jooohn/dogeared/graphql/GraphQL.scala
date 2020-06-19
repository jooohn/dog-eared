package me.jooohn.dogeared.graphql

import caliban.GraphQL.graphQL
import caliban.interop.cats.implicits._
import caliban.schema.Schema
import caliban.wrappers.Wrappers._
import caliban.{CalibanError, GraphQLInterpreter, RootResolver}
import cats.effect.ConcurrentEffect
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}
import me.jooohn.dogeared.graphql.Wrappers._
import zio.Runtime

object GraphQL {

  def interpreter[F[_]: ConcurrentEffect, R](
      twitterUserQueries: TwitterUserQueries[F],
      kindleQuotedTweetQueries: KindleQuotedTweetQueries[F],
      kindleBookQueries: KindleBookQueries[F],
      logger: Logger[F],
  )(implicit S: Schema[R, Queries[F]], R: Runtime[R]): F[GraphQLInterpreter[R, CalibanError]] = {
    val resolvers = new Resolvers[F](
      twitterUserQueries = twitterUserQueries,
      kindleBookQueries = kindleBookQueries,
      kindleQuotedTweetQueries = kindleQuotedTweetQueries,
    )
    val api = graphQL(RootResolver(resolvers.queries)) @@
      errorLogging(logger) @@
      maxDepth(10)
    api.interpreterAsync[F]
  }

}
