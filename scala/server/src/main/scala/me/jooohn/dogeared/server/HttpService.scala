package me.jooohn.dogeared.server

import caliban.Value.NullValue
import caliban._
import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.syntax.all._
import io.circe.syntax._
import me.jooohn.dogeared.drivenports.Logger
import me.jooohn.dogeared.graphql._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.{CORS, Logger => LoggerMiddleware}
import org.http4s.syntax.all._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpRoutes, Request, Response}
import zio.{Has, RIO}

case class HttpService[R <: Has[_]](
    interpreter: GraphQLInterpreter[EnvWith[R], CalibanError],
    logger: Logger,
    enableIntrospection: Boolean = true)(implicit CE: ConcurrentEffect[RIO[R, *]])
    extends Http4sDsl[RIO[R, *]] {
  object dsl extends Http4sDsl[RIO[R, *]]

  lazy val healthCheckRoutes: HttpRoutes[RIO[R, *]] = HttpRoutes.of[RIO[R, *]] {
    case GET -> Root / "healthcheck" => Ok("OK")
  }

  lazy val graphQLRoutes: HttpRoutes[RIO[R, *]] = Router(
    "/graphql" -> CORS(
      HttpRoutes.of[RIO[R, *]] {
        case req @ POST -> Root =>
          for {
            request <- req.attemptAs[GraphQLRequest].value.absolve
            requestLogger = logger.withContext(
              "graphql_operation" -> request.operationName.getOrElse(""),
              "query" -> request.query.getOrElse(""),
            )
            _ <- requestLogger.info("processing GraphQL request")
            result <- interpreter
              .executeRequest(request, skipValidation = false, enableIntrospection = enableIntrospection)
              .foldCause(cause => GraphQLResponse(NullValue, cause.defects).asJson, _.asJson)
              .provideSomeLayer[R](GraphQLContextRepository.from(req.toGraphQLContext(requestLogger)))
            _ <- requestLogger.info("GraphQL request processed")
            response <- Ok(result)
          } yield response
      },
      CORS.DefaultCORSConfig.copy(allowedOrigins = origin =>
        origin == "localhost" || origin == "jooohn.me" || origin.endsWith(".jooohn.me"))
    )
  )

  lazy val routes: Kleisli[RIO[R, *], Request[RIO[R, *]], Response[RIO[R, *]]] =
    LoggerMiddleware.httpApp(logHeaders = true, logBody = false)((healthCheckRoutes <+> graphQLRoutes).orNotFound)

  implicit class RequestOps(request: Request[RIO[R, *]]) {

    def toGraphQLContext(logger: Logger): GraphQLContext =
      GraphQLContext(
        requestType = requestType,
        logger = logger,
      )

    def requestType: RequestType =
      internalRequestSignature.fold[RequestType](RequestType.External) { signature =>
        RequestType.Internal
      }

    def internalRequestSignature: Option[String] =
      request.headers.get(CaseInsensitiveString("x-dog-eared-internal-request-signature")).map(_.value)

  }
}
