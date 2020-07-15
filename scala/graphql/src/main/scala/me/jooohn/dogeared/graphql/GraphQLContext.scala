package me.jooohn.dogeared.graphql

import me.jooohn.dogeared.drivenports.{Context, Logger}
import zio.{ZIO, ZLayer}

case class GraphQLContext(
    requestType: RequestType,
    logger: Logger
) {
  def isInternalRequest: Boolean = requestType.isInternal

  def toContext: Context = Context(logger = logger)
}

sealed abstract class RequestType(val isInternal: Boolean)
object RequestType {
  case object External extends RequestType(false)
  case object Internal extends RequestType(true)
}

case class GraphQLContextService[F[_]](graphQLContext: GraphQLContext)
object GraphQLContextRepository {

  trait Service {
    def graphQLContext: GraphQLContext
  }
  private class ServiceImpl[F[_]](override val graphQLContext: GraphQLContext) extends Service

  def getGraphQLContext[F[_]]: ZIO[GraphQLContextRepository, Nothing, GraphQLContext] =
    ZIO.access(_.get.graphQLContext)

  def from[F[_]](graphQLContext: GraphQLContext): ZLayer[Any, Nothing, GraphQLContextRepository] =
    ZLayer.succeed(new ServiceImpl(graphQLContext))
}
