package me.jooohn.dogeared.usecases

import java.net.URL

import cats.effect.IO
import cats.implicits._
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, Tweet, TwitterUser}
import me.jooohn.dogeared.drivenadapters.{
  InMemoryKindleQuotePages,
  InMemoryTwitter,
  PGKindleBooks,
  PGKindleQuotedTweets,
  PGProcessedTweets,
  PGTwitterUsers
}
import me.jooohn.dogeared.drivenports.KindleQuotePage

class ImportKindleBookQuotesSuite extends munit.FunSuite {
  import me.jooohn.dogeared._

  val testUser = TwitterUser("12345", "jooohn1234")
  val testAmazonURL = AmazonRedirectorURL(new URL("http://a.co/example"))

  override def beforeAll(): Unit = {
    new PGTwitterUsers[IO].storeMany(List(testUser)).unsafeRunSync()
  }

  test("should save KindleQuotedTweets with quote data") {
    val twitter =
      new InMemoryTwitter[IO](
        tweets = Tweet("123", testUser.id, testAmazonURL) :: Nil,
        twitterUsers = testUser :: Nil,
      )
    val processedTweets = new PGProcessedTweets[IO]

    val useCase = new ImportKindleBookQuotes[IO](
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
      twitterUsers = new PGTwitterUsers[IO],
      kindleQuotedTweets = new PGKindleQuotedTweets[IO],
      kindleBooks = new PGKindleBooks[IO],
      processedTweets = processedTweets,
    )
    useCase(testUser.id).unsafeRunSync()

    assertEquals(processedTweets.resolveByUserId(testUser.id).unsafeRunSync().map(_.latestProcessedTweetId), "123".some)
  }

}
