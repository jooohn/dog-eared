package me.jooohn.dogeared.server

import cats.effect.IO
import io.circe.Json
import me.jooohn.dogeared.server.LambdaRuntimeAPI.InvocationSuccess

object Handlers {

  val server: Handler = invocation =>
    IO {
      println(invocation)
      InvocationSuccess(Json.fromString(invocation.toString))
  }

  private[this] val map = Map[String, Handler](
    "server" -> server,
  )

  def apply(name: String): Option[Handler] = map.get(name)
}
