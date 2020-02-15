package me.jooohn

import cats.effect.{Blocker, ContextShift, IO}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.config.Config

package object dogeared {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val testConfig: Config = Config.load[IO].unsafeRunSync()

  implicit val tx: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    s"jdbc:postgresql://${testConfig.db.host}:${testConfig.db.port}/dog_eared_test",
    testConfig.db.user,
    testConfig.db.password,
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

}
