package me.jooohn.dogeared.lambda
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder
import org.http4s.Response

case class HttpApiGatewayResponse(
    isBase64Encoded: Boolean,
    statusCode: Int,
    headers: Map[String, String],
    multiValueHeaders: Map[String, List[String]],
    body: String,
)

case class EncodedResponse[F[_]](response: Response[F], body: String)

object HttpApiGatewayResponse {

  implicit val encoder: Encoder[HttpApiGatewayResponse] = deriveEncoder[HttpApiGatewayResponse]

  implicit def responseEncoder[F[_]]: Encoder[EncodedResponse[F]] =
    encoder.contramap {
      case EncodedResponse(response, body) =>
        HttpApiGatewayResponse(
          isBase64Encoded = false,
          statusCode = response.status.code,
          headers = response.headers.toList.map(header => header.name.toString() -> header.value).toMap,
          multiValueHeaders = Map.empty,
          body = body
        )
    }
}
