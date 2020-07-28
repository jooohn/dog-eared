package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.IO
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import me.jooohn.dogeared.drivenadapters.ScalaLoggingLogger
import me.jooohn.dogeared.drivenports.Logger
import me.jooohn.dogeared.testConfig
import org.scanamo.ScanamoCats

trait DynamoDBFixtures { self: munit.FunSuite =>
  val scanamoFixture = new Fixture[ScanamoCats[IO]]("dynamodb") {
    var dynamodb: AmazonDynamoDBAsync = null

    override def apply(): ScanamoCats[IO] = ScanamoCats[IO](dynamodb)

    override def beforeAll(): Unit = {
      dynamodb = AmazonDynamoDBAsyncClientBuilder
        .standard()
        .withEndpointConfiguration(
          new EndpointConfiguration(
            testConfig.aws.dynamodbEndpoint.get,
            testConfig.aws.defaultRegion,
          )
        )
        .build()
    }

    override def afterAll(): Unit = {
      dynamodb.shutdown()
    }
  }

  val logger: Logger = ScalaLoggingLogger.of("test")

  def tracingScanamo: TracingScanamo[IO] = TracingScanamo(scanamoFixture())

}
