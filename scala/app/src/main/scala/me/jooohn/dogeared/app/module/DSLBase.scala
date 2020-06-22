package me.jooohn.dogeared.app.module

import cats.effect.{ContextShift, IO, Resource, Timer}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import me.jooohn.dogeared.di.DSL

trait DSLBase extends DSL[Resource[IO, *]] {
  implicit val zioRuntime: zio.Runtime[zio.ZEnv]
  implicit val contextShift: ContextShift[IO]
  implicit val timer: Timer[IO]
  implicit val logger: Bind[Logger[IO]] = bind(Slf4jLogger.getLogger[IO])

}
