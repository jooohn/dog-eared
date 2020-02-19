package me.jooohn.dogeared.app.module

import cats.effect.{ContextShift, IO}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import com.softwaremill.macwire._
import doobie.util.transactor.Transactor
import me.jooohn.dogeared.app.TwitterConfig
import me.jooohn.dogeared.domain.AmazonRedirectorURL
import me.jooohn.dogeared.drivenadapters._
import me.jooohn.dogeared.drivenports._

trait PGAdapterModule {
  implicit val transactor: Transactor.Aux[IO, Unit]
  lazy val kindleQuotedTweets: KindleQuotedTweets[IO] = wire[PGKindleQuotedTweets[IO]]
  lazy val kindleBooks: KindleBooks[IO] = wire[PGKindleBooks[IO]]
  lazy val processedTweets: ProcessedTweets[IO] = wire[PGProcessedTweets[IO]]
  lazy val twitterUsers: TwitterUsers[IO] = wire[PGTwitterUsers[IO]]
}

trait Twitter4sAdapterModule {
  implicit val ioContextShift: ContextShift[IO]
  val twitterConfig: TwitterConfig
  val processedTweets: ProcessedTweets[IO]

  lazy val twitter4sRestClient: TwitterRestClient = TwitterRestClient(
    ConsumerToken(twitterConfig.consumerTokenKey, twitterConfig.consumerTokenSecret),
    AccessToken(twitterConfig.accessTokenKey, twitterConfig.accessTokenSecret)
  )

  lazy val tweets: Tweets[IO] = wire[Twitter4sTweets]
}

trait CrawlerAdapterModule {
  // TODO
  lazy val kindleQuotePages: KindleQuotePages[IO] = new KindleQuotePages[IO] {
    override def resolveManyByURLs(urls: List[AmazonRedirectorURL]): IO[Map[AmazonRedirectorURL, KindleQuotePage]] =
      IO.apply(???)
  }
}

trait ProductionAdapterModule extends PGAdapterModule with Twitter4sAdapterModule with CrawlerAdapterModule
