package me.jooohn.dogeared.app.module

import cats.effect.{ContextShift, IO, Timer}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.{CrawlerConfig, TwitterConfig}
import me.jooohn.dogeared.drivenadapters._
import me.jooohn.dogeared.drivenports._
import org.http4s.client.Client

trait ProductionAdapterModule {

  val twitterConfig: TwitterConfig
  val crawlerConfig: CrawlerConfig
  val ioHttp4sClient: Client[IO]
  implicit val ioTimer: Timer[IO]
  implicit val ioContextShift: ContextShift[IO]
  implicit val transactor: Transactor.Aux[IO, Unit]

  lazy val kindleQuotedTweets: KindleQuotedTweets[IO] = new PGKindleQuotedTweets[IO]
  lazy val kindleBooks: KindleBooks[IO] = new PGKindleBooks[IO]
  lazy val processedTweets: ProcessedTweets[IO] = new PGProcessedTweets[IO]
  lazy val twitterUsers: TwitterUsers[IO] = new PGTwitterUsers[IO]

  lazy val twitter4sRestClient: TwitterRestClient = TwitterRestClient(
    ConsumerToken(twitterConfig.consumerTokenKey, twitterConfig.consumerTokenSecret),
    AccessToken(twitterConfig.accessTokenKey, twitterConfig.accessTokenSecret)
  )

  lazy val tweets: Twitter[IO] = new Twitter4STwitter(
    restClient = twitter4sRestClient,
    processedTweets = processedTweets,
  )

  lazy val kindleQuotePages: KindleQuotePages[IO] = new ScraperKindleQuotePages(
    fetchInterval = crawlerConfig.intervalDuration,
    client = ioHttp4sClient
  )

}
