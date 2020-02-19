package me.jooohn.dogeared.app

import cats.effect.{Blocker, ContextShift, IO}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.module.{ProductionAdapterModule, UseCaseModules}

case class ProductionAppModule(
    transactor: Transactor.Aux[IO, Unit],
    ioContextShift: ContextShift[IO],
    twitterConfig: TwitterConfig,
) extends UseCaseModules
    with ProductionAdapterModule

object ProductionApp {

  def apply[A](run: ProductionAppModule => IO[A])(implicit CS: ContextShift[IO]): IO[A] =
    for {
      config <- Config.load[IO]
      result <- ExecutionContexts.fixedThreadPool[IO](config.db.threadPoolSize).use { dbThreadPool =>
        run(
          ProductionAppModule(
            transactor = Transactor.fromDriverManager[IO](
              "org.postgresql.Driver",
              s"jdbc:postgresql://${config.db.host}:${config.db.port}/dog_eared",
              config.db.user,
              config.db.password,
              Blocker.liftExecutionContext(dbThreadPool)
            ),
            ioContextShift = CS,
            twitterConfig = config.twitter,
          ))
      }
    } yield result

}
