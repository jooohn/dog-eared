package me.jooohn.dogeared.config

import cats.effect.{Async, ContextShift}
import ciris.ConfigValue

trait ConfigCompanion[A] {

  def configValue: ConfigValue[A]

  def load[F[_]: Async: ContextShift]: F[A] = configValue.load[F]

}
