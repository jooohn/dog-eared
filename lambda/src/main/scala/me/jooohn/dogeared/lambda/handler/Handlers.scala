package me.jooohn.dogeared.lambda.handler

import me.jooohn.dogeared.lambda.HandlerFactory

object Handlers {

  private[this] val map = Map[String, HandlerFactory](
    "server" -> Server.factory,
  )

  def factory(name: String): Option[HandlerFactory] = map.get(name)
}
