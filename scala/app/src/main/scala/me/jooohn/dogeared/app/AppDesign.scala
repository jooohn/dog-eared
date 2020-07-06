package me.jooohn.dogeared.app

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import me.jooohn.dogeared.app.module._
import zio.RIO

trait AppDesign extends DSLBase with ConfigDesign with AdapterDesign with UseCaseDesign with ServerDesign
object AppDesign {

  def apply(
      implicit ce: ConcurrentEffect[RIO[zio.ZEnv, *]],
      cs: ContextShift[RIO[zio.ZEnv, *]],
      t: Timer[RIO[zio.ZEnv, *]]): AppDesign =
    new AppDesign {
      override implicit val CE: ConcurrentEffect[Effect] = ce
      override implicit val CS: ContextShift[Effect] = cs
      override implicit val timer: Timer[Effect] = t
    }

}
