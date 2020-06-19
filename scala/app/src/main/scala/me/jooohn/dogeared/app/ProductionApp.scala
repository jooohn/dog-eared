package me.jooohn.dogeared.app

import akka.actor.ActorSystem
import cats.effect.concurrent.Semaphore
import cats.effect.{Blocker, ContextShift, IO, Resource, Timer}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.module.{ProductionAdapterModule, UseCaseModules}
import me.jooohn.dogeared.drivenadapters.FixedConcurrencyIO
import me.jooohn.dogeared.drivenports.ConcurrentIO
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.system.System

import scala.util.chaining._

case class ProductionAppModule(
    transactor: Transactor.Aux[IO, Unit],
    ioHttp4sClient: Client[IO],
    twitterClientConcurrentIO: ConcurrentIO[IO],
    config: Config,
    actorSystem: ActorSystem,
    amazonDynamoDB: AmazonDynamoDBAsync,
)(implicit override val ioTimer: Timer[IO], override val ioContextShift: ContextShift[IO])
    extends UseCaseModules
    with ProductionAdapterModule {
  override val crawlerConfig: CrawlerConfig = config.crawler
  override val twitterConfig: TwitterConfig = config.twitter
  override val awsConfig: AWSConfig = config.aws
}

object ProductionApp {
  def resource(implicit CS: ContextShift[IO], T: Timer[IO]): Resource[IO, ProductionAppModule] =
    for {
      config <- Resource.liftF(Config.load[IO])
      twitterClientConcurrentIO <- Resource.liftF(Semaphore[IO](4) map (FixedConcurrencyIO[IO]))
      dbThreadPool <- ExecutionContexts.fixedThreadPool[IO](config.db.threadPoolSize)
      http4sClient <- BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource
      actorSystem <- Resource.make(IO(ActorSystem("system")))(system =>
        IO.fromFuture(IO(system.terminate())) *> IO.unit)
      amazonDynamoDB <- Resource.make(
        IO(
          AmazonDynamoDBAsyncClientBuilder
            .standard()
            .pipe(
              builder =>
                config.aws.dynamodbEndpoint.fold(builder.withRegion(config.aws.defaultRegion))(
                  endpoint =>
                    builder.withEndpointConfiguration(
                      new EndpointConfiguration(
                        endpoint,
                        config.aws.defaultRegion,
                      )
                  )))
            .build()
        ))(client => IO(client.shutdown()))
    } yield
      ProductionAppModule(
        transactor = Transactor.fromDriverManager[IO](
          "org.postgresql.Driver",
          s"jdbc:postgresql://${config.db.host}:${config.db.port}/dog_eared",
          config.db.user,
          config.db.password,
          Blocker.liftExecutionContext(dbThreadPool)
        ),
        ioHttp4sClient = http4sClient,
        config = config,
        twitterClientConcurrentIO = twitterClientConcurrentIO,
        actorSystem = actorSystem,
        amazonDynamoDB = amazonDynamoDB,
      )

  def apply[A](run: ProductionAppModule => IO[A])(implicit CS: ContextShift[IO], T: Timer[IO]): IO[A] =
    resource.use(run)
}
