package me.jooohn.dogeared.lambda

import cats.effect.IO
import io.circe.Json
import me.jooohn.dogeared.lambda.LambdaRuntimeAPI.InvocationSuccess

object Handlers {

  val server: Handler = invocation =>
    IO {
      println(invocation)
      InvocationSuccess(Json.fromString(invocation.toString))
  }

  private[this] val map = Map[String, Handler](
    "me/jooohn/dogeared/lambda" -> server,
  )

  def apply(name: String): Option[Handler] = map.get(name)
}
