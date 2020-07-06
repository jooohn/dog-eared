package me.jooohn.dogeared.app.module

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import me.jooohn.dogeared.di.DSL
import me.jooohn.dogeared.drivenadapters.ScalaLoggingLogger
import me.jooohn.dogeared.drivenports.Logger
import zio.RIO

trait DSLBase extends DSL[Resource[RIO[zio.ZEnv, *], *]] {
  type Env = zio.ZEnv
  type Effect[A] = RIO[Env, A]
  val Effect: RIO.type = RIO

  implicit val CE: ConcurrentEffect[Effect]
  implicit val CS: ContextShift[Effect]
  implicit val timer: Timer[Effect]
  implicit lazy val logger: Bind[Logger] = bind(ScalaLoggingLogger.of("dog-eared"))
}
