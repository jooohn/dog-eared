package me.jooohn.dogeared.app

import akka.actor.ActorSystem
import cats.implicits._
import cats.effect.concurrent.Semaphore
import cats.effect.{Blocker, ContextShift, IO, Resource, Timer}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.module.{ProductionAdapterModule, UseCaseModules}
import me.jooohn.dogeared.drivenadapters.FixedConcurrencyIO
import me.jooohn.dogeared.drivenports.ConcurrentIO
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder

case class ProductionAppModule(
    transactor: Transactor.Aux[IO, Unit],
    ioHttp4sClient: Client[IO],
    twitterClientConcurrentIO: ConcurrentIO[IO],
    config: Config,
    actorSystem: ActorSystem,
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

      // TODO: Rate limiting
      twitterClientConcurrentIO <- Semaphore[IO](4) map (FixedConcurrencyIO[IO])

      result <- (for {
        dbThreadPool <- ExecutionContexts.fixedThreadPool[IO](config.db.threadPoolSize)
        http4sClient <- BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource
        actorSystem <- Resource.make(IO(ActorSystem("system")))(system =>
          IO.fromFuture(IO(system.terminate())) *> IO.unit)
      } yield
        (
          Transactor.fromDriverManager[IO](
            "org.postgresql.Driver",
            s"jdbc:postgresql://${config.db.host}:${config.db.port}/dog_eared",
            config.db.user,
            config.db.password,
            Blocker.liftExecutionContext(dbThreadPool)
          ),
          http4sClient,
          actorSystem
        )).use {
        case (transactor, http4sClient, actorSystem) =>
          run(
            ProductionAppModule(
              transactor = transactor,
              ioHttp4sClient = http4sClient,
              config = config,
              twitterClientConcurrentIO = twitterClientConcurrentIO,
              actorSystem = actorSystem,
            ))
      }
    } yield result

}
