package me.jooohn.dogeared.graphql

import me.jooohn.dogeared.drivenports.{Context, Logger, Tracer}
import zio.{ZIO, ZLayer}

case class GraphQLContext(
    requestType: RequestType,
    tracer: Tracer[Effect],
    logger: Logger
) {
  def isInternalRequest: Boolean = requestType.isInternal

  def toContext: Context[Effect] = Context(
    logger = logger,
    tracer = tracer,
  )
}

sealed abstract class RequestType(val isInternal: Boolean)
object RequestType {
  case object External extends RequestType(false)
  case object Internal extends RequestType(true)
}

object GraphQLContextRepository {

  trait Service {
    def graphQLContext: GraphQLContext
  }
  private class ServiceImpl(override val graphQLContext: GraphQLContext) extends Service

  def getGraphQLContext: ZIO[GraphQLContextRepository, Nothing, GraphQLContext] =
    ZIO.access(_.get.graphQLContext)

  def span[A](name: String)(run: Effect[A]): ZIO[Env, Throwable, A] =
    for {
      context <- getGraphQLContext
      result <- context.tracer.span(name)(run)
    } yield result

  def from(graphQLContext: GraphQLContext): ZLayer[Any, Nothing, GraphQLContextRepository] =
    ZLayer.succeed(new ServiceImpl(graphQLContext))
}
