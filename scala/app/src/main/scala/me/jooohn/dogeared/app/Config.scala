package me.jooohn.dogeared.app
import java.util.concurrent.TimeUnit

import ciris._
import cats.implicits._
import me.jooohn.dogeared.drivenadapters.dynamodb.Shard

import scala.concurrent.duration.{Duration, FiniteDuration}

case class ServerConfig(port: Int)
object ServerConfig extends ConfigCompanion[ServerConfig] {
  val configValue: ConfigValue[ServerConfig] = env("PORT").map(_.toInt).default(8080).map(ServerConfig.apply)
}

case class AWSConfig(
    defaultRegion: String,
    dynamodbEndpoint: Option[String],
    dynamodbUserShard: Shard.Size,
    dynamodbBookShard: Shard.Size,
)
object AWSConfig extends ConfigCompanion[AWSConfig] {
  val configValue: ConfigValue[AWSConfig] = (
    env("AWS_REGION"),
    env("AWS_DYNAMODB_ENDPOINT").option.default(None),
    env("AWS_DYNAMODB_USER_SHARD").map(_.toInt).default(1).map(Shard.size),
    env("AWS_DYNAMODB_BOOK_SHARD").map(_.toInt).default(1).map(Shard.size),
  ).parMapN(AWSConfig.apply)
}

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
    envOrSecret("TWITTER_CONSUMER_TOKEN_KEY"),
    envOrSecret("TWITTER_CONSUMER_TOKEN_SECRET"),
    envOrSecret("TWITTER_ACCESS_TOKEN_KEY"),
    envOrSecret("TWITTER_ACCESS_TOKEN_SECRET"),
  ).parMapN(TwitterConfig.apply)
}

case class CrawlerConfig(
    intervalDuration: FiniteDuration
)
object CrawlerConfig extends ConfigCompanion[CrawlerConfig] {
  val configValue: ConfigValue[CrawlerConfig] = env("CRAWLER_INTERVAL_MILLI_SECONDS").default("1000").as[Int].map {
    intervalMillis =>
      CrawlerConfig(
        intervalDuration = Duration(intervalMillis, TimeUnit.MILLISECONDS)
      )
  }
}

case class Config(
    aws: AWSConfig,
    db: DBConfig,
    twitter: TwitterConfig,
    crawler: CrawlerConfig,
    server: ServerConfig,
)
object Config extends ConfigCompanion[Config] {

  val configValue: ConfigValue[Config] = (
    AWSConfig.configValue,
    DBConfig.configValue,
    TwitterConfig.configValue,
    CrawlerConfig.configValue,
    ServerConfig.configValue,
  ).parMapN(Config.apply)

}
