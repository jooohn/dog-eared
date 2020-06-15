package me.jooohn.dogeared.app.module

import akka.actor.ActorSystem
import cats.effect.{ContextShift, IO, Timer}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import doobie.util.transactor.Transactor
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import me.jooohn.dogeared.app.{AWSConfig, CrawlerConfig, TwitterConfig}
import me.jooohn.dogeared.drivenadapters._
import me.jooohn.dogeared.drivenadapters.dynamodb.{
  DynamoKindleBooks,
  DynamoKindleQuotedTweets,
  DynamoProcessedTweets,
  DynamoTwitterUsers,
  DynamoUserKindleBooks
}
import me.jooohn.dogeared.drivenports._
import org.http4s.client.Client
import org.scanamo.ScanamoCats

trait ProductionAdapterModule {

  val twitterConfig: TwitterConfig
  val crawlerConfig: CrawlerConfig
  val ioHttp4sClient: Client[IO]
  val awsConfig: AWSConfig
  val amazonDynamoDB: AmazonDynamoDBAsync
  val twitterClientConcurrentIO: ConcurrentIO[IO]

  implicit val ioTimer: Timer[IO]
  implicit val ioContextShift: ContextShift[IO]
  implicit val transactor: Transactor.Aux[IO, Unit]
  implicit val actorSystem: ActorSystem

  lazy val logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  lazy val scanamo: ScanamoCats[IO] = ScanamoCats(amazonDynamoDB)
  lazy val processedTweets: ProcessedTweets[IO] =
    new DynamoProcessedTweets(scanamo, logger, awsConfig.dynamodbUserShard)

  lazy val dynamoKindleQuotedTweets = new DynamoKindleQuotedTweets(scanamo, logger)
  lazy val kindleQuotedTweets: KindleQuotedTweets[IO] = dynamoKindleQuotedTweets
  lazy val kindleQuotedTweetQueries: KindleQuotedTweetQueries[IO] = dynamoKindleQuotedTweets

  lazy val dynamoKindleBooks = new DynamoKindleBooks(scanamo, logger, awsConfig.dynamodbBookShard)
  lazy val kindleBooks: KindleBooks[IO] = dynamoKindleBooks
  lazy val kindleBookQueries: KindleBookQueries[IO] = dynamoKindleBooks

  lazy val dynamoTwitterUsers = new DynamoTwitterUsers(scanamo, logger, awsConfig.dynamodbUserShard)
  lazy val twitterUsers: TwitterUsers[IO] = dynamoTwitterUsers
  lazy val twitterUserQueries: TwitterUserQueries[IO] = dynamoTwitterUsers

  lazy val userKindleBooks: UserKindleBooks[IO] = new DynamoUserKindleBooks(scanamo, logger)

  lazy val twitter4sRestClient: TwitterRestClient = TwitterRestClient.withActorSystem(
    ConsumerToken(twitterConfig.consumerTokenKey, twitterConfig.consumerTokenSecret),
    AccessToken(twitterConfig.accessTokenKey, twitterConfig.accessTokenSecret)
  )(actorSystem)

  lazy val tweets: Twitter[IO] = new Twitter4STwitter(
    restClient = twitter4sRestClient,
    processedTweets = processedTweets,
    concurrentIO = twitterClientConcurrentIO,
  )

  lazy val kindleQuotePages: KindleQuotePages[IO] = new ScraperKindleQuotePages(
    fetchInterval = crawlerConfig.intervalDuration,
    client = ioHttp4sClient
  )

}
