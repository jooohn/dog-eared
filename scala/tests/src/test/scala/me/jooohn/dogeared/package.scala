package me.jooohn

import cats.effect.{Blocker, ContextShift, IO}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.{AWSConfig, Config, CrawlerConfig, DBConfig, ServerConfig, TwitterConfig}

package object dogeared {

  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContexts.synchronous)

  val testConfig: Config = Config(
    db = DBConfig.load[IO].unsafeRunSync(),
    twitter = TwitterConfig(
      consumerTokenKey = "dummy",
      consumerTokenSecret = "dummy",
      accessTokenKey = "dummy",
      accessTokenSecret = "dummy",
    ),
    aws = AWSConfig.load[IO].unsafeRunSync(),
    crawler = CrawlerConfig.load[IO].unsafeRunSync(),
    server = ServerConfig(8080),
  )

  implicit val tx: Transactor.Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    s"jdbc:postgresql://${testConfig.db.host}:${testConfig.db.port}/dog_eared_test",
    testConfig.db.user,
    testConfig.db.password,
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

}
