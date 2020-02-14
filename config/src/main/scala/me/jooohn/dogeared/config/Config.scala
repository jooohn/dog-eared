package me.jooohn.dogeared.config
import ciris._
import cats.implicits._

case class DBConfig(
    host: String,
    port: Int,
    user: String,
    password: String
)
object DBConfig extends ConfigCompanion[DBConfig] {
  val configValue: ConfigValue[DBConfig] = (
    env("DB_HOST").default("localhost"),
    env("DB_PORT").default("5432").as[Int],
    env("DB_USER").default("postgres"),
    env("DB_PASSWORD").default("")
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

case class Config(
    db: DBConfig,
    twitter: TwitterConfig
)
object Config extends ConfigCompanion[Config] {

  val configValue: ConfigValue[Config] = (
    DBConfig.configValue,
    TwitterConfig.configValue
  ).parMapN(Config.apply)

}
