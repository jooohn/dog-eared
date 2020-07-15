package me.jooohn.dogeared.app.module

import akka.actor.ActorSystem
import cats.effect.Resource
import cats.effect.concurrent.Semaphore
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import me.jooohn.dogeared.app.{AWSConfig, CrawlerConfig, TwitterConfig}
import me.jooohn.dogeared.drivenadapters._
import me.jooohn.dogeared.drivenadapters.dynamodb._
import me.jooohn.dogeared.drivenports._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.scanamo.ScanamoCats
import zio.interop.catz.core._

import scala.util.chaining._

trait AdapterDesign { self: DSLBase with ConfigDesign =>

  implicit def actorSystem: Bind[ActorSystem] =
    injectF(Resource.make(Effect(ActorSystem("system")))(system =>
      Effect.fromFuture(_ => system.terminate()) *> Effect.unit)).singleton

  implicit def ioHttp4sClient: Bind[Client[Effect]] =
    injectF(BlazeClientBuilder[Effect](scala.concurrent.ExecutionContext.Implicits.global).resource).singleton

  implicit def scanamo: Bind[ScanamoCats[Effect]] = inject[AmazonDynamoDBAsync].map(ScanamoCats[Effect]).singleton

  implicit def amazonDynamoDB: Bind[AmazonDynamoDBAsync] =
    singleton(for {
      config <- inject[AWSConfig]
      amazonDynamoDB <- injectF(
        Resource.make(
          Effect(
            AmazonDynamoDBAsyncClientBuilder
              .standard()
              .pipe(builder =>
                config.dynamodbEndpoint.fold(builder.withRegion(config.defaultRegion))(endpoint =>
                  builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, config.defaultRegion))))
              .build()
          ))(client => Effect(client.shutdown())))
    } yield amazonDynamoDB)

  implicit def processedTweets: Bind[ProcessedTweets[Effect]] =
    singleton(for {
      scanamo <- inject[ScanamoCats[Effect]]
      logger <- inject[Logger]
      awsConfig <- inject[AWSConfig]
    } yield new DynamoProcessedTweets[Effect](scanamo, logger, awsConfig.dynamodbUserShard))

  implicit def dynamoKindleQuotedTweets: Bind[DynamoKindleQuotedTweets[Effect]] =
    derive[DynamoKindleQuotedTweets[Effect]].singleton
  implicit def kindleQuotedTweets: Bind[KindleQuotedTweets[Effect]] = dynamoKindleQuotedTweets.widen
  implicit def kindleQuotedTweetQueries: Bind[KindleQuotedTweetQueries[Effect]] = dynamoKindleQuotedTweets.widen

  implicit def dynamoKindleBooks: Bind[DynamoKindleBooks[Effect]] =
    singleton(for {
      scanamo <- inject[ScanamoCats[Effect]]
      logger <- inject[Logger]
      awsConfig <- inject[AWSConfig]
    } yield DynamoKindleBooks[Effect](scanamo, logger, awsConfig.dynamodbBookShard))
  implicit def kindleBooks: Bind[KindleBooks[Effect]] = dynamoKindleBooks.widen
  implicit def kindleBookQueries: Bind[KindleBookQueries[Effect]] = dynamoKindleBooks.widen

  implicit def dynamoTwitterUsers: Bind[DynamoTwitterUsers[Effect]] =
    singleton(for {
      scanamo <- inject[ScanamoCats[Effect]]
      logger <- inject[Logger]
      awsConfig <- inject[AWSConfig]
    } yield DynamoTwitterUsers(scanamo, logger, awsConfig.dynamodbUserShard))
  implicit def twitterUsers: Bind[TwitterUsers[Effect]] = dynamoTwitterUsers.widen
  implicit def twitterUserQueries: Bind[TwitterUserQueries[Effect]] = dynamoTwitterUsers.widen

  implicit def userKindleBooks: Bind[UserKindleBooks[Effect]] = derive[DynamoUserKindleBooks[Effect]].singleton.widen

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

  implicit def tweets: Bind[Twitter[Effect]] =
    singleton(for {
      restClient <- inject[TwitterRestClient]
      processedTweets <- inject[ProcessedTweets[Effect]]
      concurrentEffect <- injectF(FixedConcurrentExecutor.resource(4))
    } yield Twitter4STwitter(restClient, processedTweets, concurrentEffect))

  implicit def kindleQuotePages: Bind[KindleQuotePages[Effect]] =
    singleton(
      for {
        client <- inject[Client[Effect]]
        crawlerConfig <- inject[CrawlerConfig]
      } yield
        new ScraperKindleQuotePages(
          client = client,
          fetchInterval = crawlerConfig.intervalDuration,
        ))

}
