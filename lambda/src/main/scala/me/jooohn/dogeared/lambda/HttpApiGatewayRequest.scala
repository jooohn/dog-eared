package me.jooohn.dogeared.lambda

import cats.effect.IO
import cats.implicits._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, JsonObject}
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s._
import org.http4s.util.CaseInsensitiveString

case class HttpApiGatewayRequest(
    version: String,
    routeKey: String,
    rawPath: String,
    rawQueryString: String,
    headers: Map[String, String],
    requestContext: HttpApiGatewayRequest.RequestContext,
    isBase64Encoded: Boolean,
    cookies: Option[List[String]],
    queryStringParameters: Option[JsonObject],
    body: Option[String],
    pathParameters: Option[JsonObject],
    stageVariables: Option[JsonObject],
)

object HttpApiGatewayRequest {
  implicit val httpDecoder: Decoder[Http] = deriveDecoder[Http]
  implicit val requestContextDecoder: Decoder[RequestContext] = deriveDecoder[RequestContext]
  implicit val httpApiGatewayPayloadDecoder: Decoder[HttpApiGatewayRequest] = deriveDecoder[HttpApiGatewayRequest]

  implicit def http4sIORequestDecoder[F[_]: EntityEncoder[*[_], String]]: Decoder[Request[F]] =
    httpApiGatewayPayloadDecoder.emap { payload =>
      (for {
        method <- Method.fromString(payload.requestContext.http.method)
        httpVersion <- HttpVersion.fromString(payload.requestContext.http.protocol)
        headers = Headers.of(payload.headers.toList.map(Function.tupled(Header.apply)): _*)
        scheme <- Scheme.fromString(headers.get(CaseInsensitiveString("x-forwarded-proto")).fold("http")(_.value))
        uri = Uri(
          scheme = Some(scheme),
          authority = Some(
            Authority(
              host = RegName(payload.requestContext.domainName),
            )),
          path = payload.requestContext.http.path,
          query = Query.fromString(payload.rawQueryString)
        )
      } yield
        Request[F]()
          .withUri(uri)
          .withHttpVersion(httpVersion)
          .withMethod(method)
          .withEntity(payload.body.getOrElse(""))
          .withHeaders(headers)).leftMap(failure => failure.message)
    }

  case class RequestContext(
      accountId: String,
      authorizer: Option[JsonObject],
      apiId: String,
      domainName: String,
      domainPrefix: String,
      http: Http,
      requestId: String,
      routeKey: String,
      stage: String,
      time: String,
      timeEpoch: Long
  )

  case class Http(
      method: String,
      path: String,
      protocol: String,
      sourceIp: String,
      userAgent: String,
  )

}
