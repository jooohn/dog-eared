package me.jooohn.dogeared.app

import cats.effect.{Blocker, ContextShift, IO, Timer}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.module.{ProductionAdapterModule, UseCaseModules}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

case class ProductionAppModule(
    transactor: Transactor.Aux[IO, Unit],
    ioHttp4sClient: Client[IO],
    config: Config,
)(implicit override val ioTimer: Timer[IO], override val ioContextShift: ContextShift[IO])
    extends UseCaseModules
    with ProductionAdapterModule {

  override val crawlerConfig: CrawlerConfig = config.crawler
  override val twitterConfig: TwitterConfig = config.twitter
}

object ProductionApp {

  def apply[A](run: ProductionAppModule => IO[A])(implicit CS: ContextShift[IO], T: Timer[IO]): IO[A] =
    for {
      config <- Config.load[IO]
      result <- (for {
        dbThreadPool <- ExecutionContexts.fixedThreadPool[IO](config.db.threadPoolSize)
        http4sClient <- BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource
      } yield
        (
          Transactor.fromDriverManager[IO](
            "org.postgresql.Driver",
            s"jdbc:postgresql://${config.db.host}:${config.db.port}/dog_eared",
            config.db.user,
            config.db.password,
            Blocker.liftExecutionContext(dbThreadPool)
          ),
          http4sClient
        )).use {
        case (transactor, http4sClient) =>
          run(
            ProductionAppModule(
              transactor = transactor,
              ioHttp4sClient = http4sClient,
              config = config,
            ))
      }
    } yield result

}
