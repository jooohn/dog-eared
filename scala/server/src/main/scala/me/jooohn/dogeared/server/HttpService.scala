package me.jooohn.dogeared.server

import caliban.Value.NullValue
import caliban._
import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.syntax.all._
import com.amazonaws.xray.entities.TraceHeader
import io.circe.syntax._
import me.jooohn.dogeared.drivenadapters.{TracingRequest, TracingResponse, XRayTracer}
import me.jooohn.dogeared.drivenports.{Logger, Tracer}
import me.jooohn.dogeared.graphql._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.{CORS, Logger => LoggerMiddleware}
import org.http4s.syntax.all._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{HttpRoutes, Request, Response}

case class HttpService(
    interpreter: GraphQLInterpreter[Env, CalibanError],
    logger: Logger,
    baseDomainName: String,
    xrayTracer: XRayTracer[Effect],
    enableIntrospection: Boolean = true)(implicit CE: ConcurrentEffect[Effect])
    extends Http4sDsl[Effect] {
  type Service[F[_]] = Kleisli[F, Request[F], Response[F]]
  object dsl extends Http4sDsl[Effect]

  lazy val healthCheckRoutes: HttpRoutes[Effect] = HttpRoutes.of[Effect] {
    case GET -> Root / "healthcheck" => Ok("OK")
  }

  def graphQLRoutes(tracer: Tracer[Effect]): HttpRoutes[Effect] = Router(
    "/graphql" -> CORS(
      HttpRoutes.of[Effect] {
        case req @ POST -> Root =>
          for {
            request <- req.attemptAs[GraphQLRequest].value.absolve
            requestLogger = logger.withContext(
              "graphql_operation" -> request.operationName.getOrElse(""),
              "query" -> request.query.getOrElse(""),
            )
            _ <- requestLogger.info("processing GraphQL request")
            result <- tracer.span("graphql") {
              interpreter
                .executeRequest(request, skipValidation = false, enableIntrospection = enableIntrospection)
                .foldCause(cause => GraphQLResponse(NullValue, cause.defects).asJson, _.asJson)
                .provideSomeLayer[zio.ZEnv](
                  GraphQLContextRepository.from(
                    req.toGraphQLContext(
                      tracer = tracer,
                      logger = requestLogger,
                    )))
            }
            _ <- requestLogger.info("GraphQL request processed")
            response <- Ok(result)
          } yield response
      },
      CORS.DefaultCORSConfig.copy(allowedOrigins = origin =>
        origin == "localhost" || origin == baseDomainName || origin.endsWith(s".${baseDomainName}"))
    )
  )

  def tracerMiddleware(underlying: Tracer[Effect] => Service[Effect]): Service[Effect] =
    Kleisli { request =>
      val context = request.headers.get(CaseInsensitiveString("x-amzn-trace-id")) map { header =>
        TraceHeader.fromString(header.value)
      }
      val tracingRequest = TracingRequest.Http(
        method = request.method.toString(),
        url = request.uri.toString()
      )
      xrayTracer.segment("http4s", tracingRequest, context) { tracer =>
        underlying(tracer).run(request) map { response =>
          val tracingResponse = TracingResponse.Http(
            status = response.status.code
          )
          (tracingResponse, response)
        }
      }
    }

  lazy val routes: Service[Effect] =
    tracerMiddleware { segment =>
      (healthCheckRoutes <+> LoggerMiddleware
        .httpRoutes(logHeaders = true, logBody = false)(graphQLRoutes(segment))).orNotFound
    }

  implicit class RequestOps(request: Request[Effect]) {

    def toGraphQLContext(logger: Logger, tracer: Tracer[Effect]): GraphQLContext =
      GraphQLContext(
        requestType = requestType,
        tracer = tracer,
        logger = logger,
      )

    def requestType: RequestType =
      internalRequestSignature.fold[RequestType](RequestType.External) { signature =>
        // TODO
        RequestType.Internal
      }

    def internalRequestSignature: Option[String] =
      request.headers.get(CaseInsensitiveString("x-dog-eared-internal-request-signature")).map(_.value)

  }
}
