package me.jooohn.dogeared.lambda.handler

import cats.data.{EitherT, Kleisli}
import cats.effect.{ContextShift, IO}
import cats.implicits._
import fs2.Collector
import io.circe.{Decoder, Encoder}
import me.jooohn.dogeared.graphql.GraphQL
import me.jooohn.dogeared.lambda.LambdaRuntimeApi.{
  InvocationFailure,
  InvocationRequest,
  InvocationResponse,
  InvocationSuccess
}
import me.jooohn.dogeared.lambda._
import me.jooohn.dogeared.server.HttpService
import org.http4s.{Request, Response}
import caliban.interop.cats.implicits._
import zio.interop.catz._

case class Server(httpService: Kleisli[IO, Request[IO], Response[IO]])(
    implicit CS: ContextShift[IO],
    R: zio.Runtime[zio.ZEnv])
    extends Handler {
  import HttpApiGatewayRequest._
  import HttpApiGatewayResponse._

  val decoder: Decoder[Request[IO]] = Decoder[Request[IO]]
  val encoder: Encoder[EncodedResponse[IO]] = Encoder[EncodedResponse[IO]]

  type Effect[A] = EitherT[IO, InvocationResponse, A]

  def decodeRequest(invocationRequest: InvocationRequest): Effect[Request[IO]] =
    EitherT.fromEither(
      decoder
        .decodeJson(invocationRequest.payload)
        .leftMap(e => InvocationFailure(errorType = "InvalidPayload", errorMessage = e.message)))

  def run(request: Request[IO]): Effect[Response[IO]] = EitherT.liftF(httpService.run(request))

  def encodeResponse(response: Response[IO]): Effect[InvocationResponse] =
    EitherT.liftF(response.bodyAsText.compile.to(Collector.string)) map { body =>
      InvocationSuccess(encoder(EncodedResponse(response, body)))
    }

  override def apply(invocationRequest: InvocationRequest): IO[InvocationResponse] =
    (for {
      request <- decodeRequest(invocationRequest)
      httpResponse <- run(request)
      invocationResponse <- encodeResponse(httpResponse)
    } yield invocationResponse).value.map(_.fold(identity, identity))
}

object Server {

  val factory: HandlerFactory = module => {
    import module.ioContextShift
    implicit val runtime: zio.Runtime[zio.ZEnv] = zio.Runtime.default
    GraphQL.interpreter[IO, zio.ZEnv](
      twitterUserQueries = module.twitterUserQueries,
      kindleBookQueries = module.kindleBookQueries,
      kindleQuotedTweetQueries = module.kindleQuotedTweetQueries,
      logger = module.logger,
    ) map (interpreter => Server(HttpService[IO, zio.ZEnv](interpreter)))
  }

}
