package me.jooohn.dogeared.app.module

import akka.actor.ActorSystem
import cats.effect.concurrent.Semaphore
import cats.effect.{IO, Resource}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import io.chrisdavenport.log4cats.Logger
import me.jooohn.dogeared.app.{AWSConfig, CrawlerConfig, TwitterConfig}
import me.jooohn.dogeared.drivenadapters._
import me.jooohn.dogeared.drivenadapters.dynamodb._
import me.jooohn.dogeared.drivenports._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.scanamo.ScanamoCats

import scala.util.chaining._

trait AdapterDesign { self: DSLBase with ConfigDesign =>

  implicit def actorSystem: Bind[ActorSystem] =
    injectF(Resource.make(IO(ActorSystem("system")))(system => IO.fromFuture(IO(system.terminate())) *> IO.unit)).singleton

  implicit def ioHttp4sClient: Bind[Client[IO]] =
    injectF(BlazeClientBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global).resource).singleton

  implicit def scanamo: Bind[ScanamoCats[IO]] = inject[AmazonDynamoDBAsync].map(ScanamoCats[IO]).singleton

  implicit def amazonDynamoDB: Bind[AmazonDynamoDBAsync] =
    singleton(for {
      config <- inject[AWSConfig]
      amazonDynamoDB <- injectF(
        Resource.make(
          IO(
            AmazonDynamoDBAsyncClientBuilder
              .standard()
              .pipe(builder =>
                config.dynamodbEndpoint.fold(builder.withRegion(config.defaultRegion))(endpoint =>
                  builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, config.defaultRegion))))
              .build()
          ))(client => IO(client.shutdown())))
    } yield amazonDynamoDB)

  implicit def processedTweets: Bind[ProcessedTweets[IO]] =
    singleton(for {
      scanamo <- inject[ScanamoCats[IO]]
      logger <- inject[Logger[IO]]
      awsConfig <- inject[AWSConfig]
    } yield new DynamoProcessedTweets[IO](scanamo, logger, awsConfig.dynamodbUserShard))

  implicit def dynamoKindleQuotedTweets: Bind[DynamoKindleQuotedTweets[IO]] =
    derive[DynamoKindleQuotedTweets[IO]].singleton
  implicit def kindleQuotedTweets: Bind[KindleQuotedTweets[IO]] = dynamoKindleQuotedTweets.widen
  implicit def kindleQuotedTweetQueries: Bind[KindleQuotedTweetQueries[IO]] = dynamoKindleQuotedTweets.widen

  implicit def dynamoKindleBooks: Bind[DynamoKindleBooks[IO]] =
    singleton(for {
      scanamo <- inject[ScanamoCats[IO]]
      logger <- inject[Logger[IO]]
      awsConfig <- inject[AWSConfig]
    } yield DynamoKindleBooks[IO](scanamo, logger, awsConfig.dynamodbBookShard))
  implicit def kindleBooks: Bind[KindleBooks[IO]] = dynamoKindleBooks.widen
  implicit def kindleBookQueries: Bind[KindleBookQueries[IO]] = dynamoKindleBooks.widen

  implicit def dynamoTwitterUsers: Bind[DynamoTwitterUsers[IO]] =
    singleton(for {
      scanamo <- inject[ScanamoCats[IO]]
      logger <- inject[Logger[IO]]
      awsConfig <- inject[AWSConfig]
    } yield DynamoTwitterUsers(scanamo, logger, awsConfig.dynamodbUserShard))
  implicit def twitterUsers: Bind[TwitterUsers[IO]] = dynamoTwitterUsers.widen
  implicit def twitterUserQueries: Bind[TwitterUserQueries[IO]] = dynamoTwitterUsers.widen

  implicit def userKindleBooks: Bind[UserKindleBooks[IO]] = derive[DynamoUserKindleBooks[IO]].singleton.widen

  implicit def twitter4sRestClient: Bind[TwitterRestClient] =
    singleton(
      for {
        twitterConfig <- inject[TwitterConfig]
        actorSystem <- inject[ActorSystem]
      } yield
        TwitterRestClient.withActorSystem(
          ConsumerToken(twitterConfig.consumerTokenKey, twitterConfig.consumerTokenSecret),
          AccessToken(twitterConfig.accessTokenKey, twitterConfig.accessTokenSecret)
        )(actorSystem))

  implicit def tweets: Bind[Twitter[IO]] =
    singleton(for {
      restClient <- inject[TwitterRestClient]
      processedTweets <- inject[ProcessedTweets[IO]]
      concurrentIO <- injectF(Resource.liftF(Semaphore[IO](4) map FixedConcurrencyIO[IO]))
    } yield Twitter4STwitter(restClient, processedTweets, concurrentIO))

  implicit def kindleQuotePages: Bind[KindleQuotePages[IO]] =
    singleton(
      for {
        client <- inject[Client[IO]]
        crawlerConfig <- inject[CrawlerConfig]
      } yield
        new ScraperKindleQuotePages(
          client = client,
          fetchInterval = crawlerConfig.intervalDuration,
        ))

}
