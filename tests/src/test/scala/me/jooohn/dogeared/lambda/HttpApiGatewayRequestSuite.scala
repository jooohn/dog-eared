package me.jooohn.dogeared.lambda

import cats.effect.IO
import io.circe.Decoder
import io.circe.parser._
import org.http4s.{HttpVersion, Request}

class HttpApiGatewayRequestSuite extends munit.FunSuite {

  test("it converts payload to http4s Request") {
    import me.jooohn.dogeared.lambda.HttpApiGatewayRequest._
    val rawJSON =
      """
        |{
        |  "version" : "2.0",
        |  "routeKey" : "$default",
        |  "rawPath" : "/",
        |  "rawQueryString" : "",
        |  "headers" : {
        |    "accept" : "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
        |    "accept-encoding" : "gzip, deflate, br",
        |    "accept-language" : "en-US,en;q=0.9,ja;q=0.8",
        |    "cache-control" : "max-age=0",
        |    "content-length" : "0",
        |    "host" : "example.com",
        |    "sec-fetch-dest" : "document",
        |    "sec-fetch-mode" : "navigate",
        |    "sec-fetch-site" : "none",
        |    "sec-fetch-user" : "?1",
        |    "upgrade-insecure-requests" : "1",
        |    "user-agent" : "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36",
        |    "x-amzn-trace-id" : "Root=1-5ee8d9f7-0afb79c415cd9e1f494c3e59",
        |    "x-forwarded-for" : "118.241.142.198",
        |    "x-forwarded-port" : "443",
        |    "x-forwarded-proto" : "https"
        |  },
        |  "requestContext" : {
        |    "accountId" : "156556316535",
        |    "apiId" : "mixys8t9le",
        |    "domainName" : "example.com",
        |    "domainPrefix" : "",
        |    "http" : {
        |      "method" : "GET",
        |      "path" : "/",
        |      "protocol" : "HTTP/1.1",
        |      "sourceIp" : "118.241.142.198",
        |      "userAgent" : "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.97 Safari/537.36"
        |    },
        |    "requestId" : "OOb-pj_BNjMEMPw=",
        |    "routeKey" : "$default",
        |    "stage" : "$default",
        |    "time" : "16/Jun/2020:14:40:55 +0000",
        |    "timeEpoch" : 1592318455264
        |  },
        |  "isBase64Encoded" : false
        |}
        |""".stripMargin
    val request = Decoder[Request[IO]].decodeJson(parse(rawJSON).toOption.get).toOption.get
    assertEquals(request.uri.toString(), "https://example.com/?")
    assertEquals(request.httpVersion, HttpVersion.`HTTP/1.1`)
  }

}
