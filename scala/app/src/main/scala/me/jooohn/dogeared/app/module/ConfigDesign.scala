package me.jooohn.dogeared.app.module

import cats.effect.{IO, Resource}
import me.jooohn.dogeared.app.{AWSConfig, Config, CrawlerConfig, DBConfig, ServerConfig, TwitterConfig}

trait ConfigDesign { self: DSLBase =>

  implicit def config: Bind[Config] = bindF(Resource.liftF(Config.load[IO])).singleton

  implicit def awsConfig: Bind[AWSConfig] = config.map(_.aws)
  implicit def dbConfig: Bind[DBConfig] = config.map(_.db)
  implicit def twitterConfig: Bind[TwitterConfig] = config.map(_.twitter)
  implicit def crawlerConfig: Bind[CrawlerConfig] = config.map(_.crawler)
  implicit def serverConfig: Bind[ServerConfig] = config.map(_.server)

}
