package me.jooohn.dogeared.usecases

import java.net.URL
import java.util.UUID

import cats.effect.IO
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, Tweet, TwitterUser}
import me.jooohn.dogeared.drivenadapters.dynamodb._
import me.jooohn.dogeared.drivenadapters.{InMemoryKindleQuotePages, InMemoryTwitter, PGTwitterUsers}
import me.jooohn.dogeared.drivenports.KindleQuotePage

class ImportKindleBookQuotesForTwitterUserSuite extends munit.FunSuite with DynamoDBFixtures {
  import me.jooohn.dogeared._

  override def munitFixtures = List(scanamoFixture)

  val testAmazonURL = AmazonRedirectorURL(new URL("http://a.co/example"))

  test("should save KindleQuotedTweets with quote data") {
    val testUserId = UUID.randomUUID().toString
    val testUser = TwitterUser(testUserId, "jooohn1234")
    val twitter =
      new InMemoryTwitter[IO](
        tweets = Tweet(testUserId, testUser.id, testAmazonURL) :: Nil,
        twitterUsers = testUser :: Nil,
      )
    val logger = Slf4jLogger.getLogger[IO]
    val processedTweets = new DynamoProcessedTweets[IO](scanamoFixture(), logger, 1)

    val useCase = new ImportKindleBookQuotesForUser[IO](
      twitter = twitter,
      kindleQuotePages = new InMemoryKindleQuotePages[IO](
        Map(
          testAmazonURL -> KindleQuotePage(
            bookId = "book-1",
            bookTitle = "FP is awesome",
            bookAuthors = List("F.P."),
            bookURL = new URL("https://example.com?book"),
            quotePageURL = new URL("https:/example.com?quote"),
            quoteBody = "abc",
          )
        )),
      twitterUsers = new DynamoTwitterUsers(scanamoFixture(), logger, 1),
      userKindleBooks = new DynamoUserKindleBooks(scanamoFixture(), logger),
      kindleQuotedTweets = new DynamoKindleQuotedTweets(scanamoFixture(), logger),
      kindleBooks = new DynamoKindleBooks(scanamoFixture(), logger, 1),
      processedTweets = processedTweets,
    )
    useCase(testUser.id).unsafeRunSync()

    assertEquals(
      processedTweets.resolveByUserId(testUser.id).unsafeRunSync().map(_.latestProcessedTweetId),
      testUserId.some)
  }

}
