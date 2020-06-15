package me.jooohn.dogeared.lambda

import cats.effect.IO
import io.circe.{Encoder, Json}
import org.http4s.Method._
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, EntityEncoder, Header, Response, Uri}
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.client.dsl.io._
import org.http4s.util.CaseInsensitiveString

// https://docs.aws.amazon.com/lambda/latest/dg/runtimes-api.html#runtimes-api-next
class LambdaRuntimeAPI(baseUri: Uri, client: Client[IO]) {
  import LambdaRuntimeAPI._

  val runtimeAPIVersion = "2018-06-01"

  val invocationErrorUri: Uri = baseUri / runtimeAPIVersion / "runtime" / "init" / "error"
  val nextInvocationUri: Uri = baseUri / runtimeAPIVersion / "runtime" / "invocation" / "next"
  def invocationResponseUri(requestId: RequestId): Uri =
    baseUri / runtimeAPIVersion / "runtime" / "invocation" / requestId / "response"
  def invocationErrorUri(requestId: RequestId): Uri =
    baseUri / runtimeAPIVersion / "runtime" / "invocation" / requestId / "error"

  def initializationError(failure: InvocationFailure): IO[Unit] = {
    val request = POST(failure, invocationErrorUri, Header("Lambda-Runtime-Function-Error-Type", failure.errorType))
    client.expect[Unit](request)
  }

  def nextInvocation: IO[InvocationRequest] =
    client.get(nextInvocationUri) { response =>
      if (response.status.isSuccess) {
        for {
          requestId <- response.requestId
          body <- EntityDecoder[IO, Json].decode(response, strict = false).rethrowT
        } yield InvocationRequest(requestId, response.traceId, body)
      } else {
        IO.raiseError(new RuntimeException(s"Failed to fetch ${nextInvocationUri}: ${response.status}"))
      }
    }

  def invocationResponse(requestId: RequestId, success: InvocationSuccess): IO[Unit] =
    client.expect[Unit](POST(success.body, invocationResponseUri(requestId)))

  def invocationError(requestId: RequestId, failure: InvocationFailure): IO[Unit] =
    client.expect[Unit](
      POST(
        failure,
        invocationErrorUri(requestId),
        Header("Lambda-Runtime-Function-Error-Type", failure.errorType),
      ))

  implicit class ResponseOps(response: Response[IO]) {

    def requestId: IO[RequestId] = unsafeHeader("Lambda-Runtime-Aws-Request-Id")
    def traceId: Option[String] = header("Lambda-Runtime-Trace-Id")

    def header(key: String): Option[String] = response.headers.find(_.name == CaseInsensitiveString(key)).map(_.value)

    def unsafeHeader(key: String): IO[String] =
      IO.fromOption(header(key))(new RuntimeException(s"${key} should be included in header"))

  }
}

object LambdaRuntimeAPI {
  type RequestId = String

  case class InvocationRequest(
      requestId: RequestId,
      traceId: Option[String],
      payload: Json,
  )

  sealed trait InvocationResponse

  case class InvocationSuccess(body: Json) extends InvocationResponse

  case class InvocationFailure(errorType: String, errorMessage: String) extends InvocationResponse
  object InvocationFailure {
    implicit val invocationFailureEncoder: EntityEncoder[IO, InvocationFailure] =
      jsonEncoderOf[IO, InvocationFailure]
  }

}
