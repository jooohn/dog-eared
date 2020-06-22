package me.jooohn.dogeared.app

import cats.effect.{ContextShift, IO, Timer}
import me.jooohn.dogeared.app.module._

trait AppDesign extends DSLBase with ConfigDesign with AdapterDesign with UseCaseDesign with ServerDesign
object AppDesign {

  def apply(implicit cs: ContextShift[IO], t: Timer[IO], z: zio.Runtime[zio.ZEnv]): AppDesign = new AppDesign {
    override implicit val zioRuntime: zio.Runtime[zio.ZEnv] = z
    override implicit val contextShift: ContextShift[IO] = cs
    override implicit val timer: Timer[IO] = t
  }

}
