package me.jooohn.dogeared.app
import java.util.concurrent.TimeUnit

import ciris._
import cats.implicits._

import scala.concurrent.duration.{Duration, FiniteDuration}

case class DBConfig(
    host: String,
    port: Int,
    user: String,
    password: String,
    threadPoolSize: Int,
)
object DBConfig extends ConfigCompanion[DBConfig] {
  val configValue: ConfigValue[DBConfig] = (
    env("DB_HOST").default("localhost"),
    env("DB_PORT").default("5432").as[Int],
    env("DB_USER").default("postgres"),
    env("DB_PASSWORD").default(""),
    env("DB_THREAD_POOL_SIZE").default("30").as[Int],
  ).parMapN(DBConfig.apply)
}

case class TwitterConfig(
    consumerTokenKey: String,
    consumerTokenSecret: String,
    accessTokenKey: String,
    accessTokenSecret: String
)
object TwitterConfig {
  val configValue: ConfigValue[TwitterConfig] = (
    env("TWITTER_CONSUMER_TOKEN_KEY"),
    env("TWITTER_CONSUMER_TOKEN_SECRET"),
    env("TWITTER_ACCESS_TOKEN_KEY"),
    env("TWITTER_ACCESS_TOKEN_SECRET")
  ).parMapN(TwitterConfig.apply)
}

case class CrawlerConfig(
    intervalDuration: FiniteDuration
)
object CrawlerConfig {
  val configValue: ConfigValue[CrawlerConfig] = env("CRAWLER_INTERVAL_MILLI_SECONDS").default("1000").as[Int].map {
    intervalMillis =>
      CrawlerConfig(
        intervalDuration = Duration(intervalMillis, TimeUnit.MILLISECONDS)
      )
  }
}

case class Config(
    db: DBConfig,
    twitter: TwitterConfig,
    crawler: CrawlerConfig,
)
object Config extends ConfigCompanion[Config] {

  val configValue: ConfigValue[Config] = (
    DBConfig.configValue,
    TwitterConfig.configValue,
    CrawlerConfig.configValue
  ).parMapN(Config.apply)

}
