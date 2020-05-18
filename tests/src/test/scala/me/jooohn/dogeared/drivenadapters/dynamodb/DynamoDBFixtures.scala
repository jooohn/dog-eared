package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.IO
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.dynamodbv2.{AmazonDynamoDBAsync, AmazonDynamoDBAsyncClientBuilder}
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scanamo.ScanamoCats
import me.jooohn.dogeared.testConfig

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

  val ioLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

}
