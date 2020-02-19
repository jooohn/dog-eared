package me.jooohn.dogeared.usecases

import java.net.URL

import cats.effect.IO
import me.jooohn.dogeared.domain.{AmazonRedirectorURL, Tweet, TwitterUser}
import me.jooohn.dogeared.drivenadapters.{
  InMemoryKindleQuotePages,
  InMemoryTweets,
  PGKindleBooks,
  PGKindleQuotedTweets,
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
    val tweets =
      new InMemoryTweets[IO](Tweet("123", testUser.id, testAmazonURL) :: Nil)
    val useCase = new ImportKindleBookQuotes[IO](
      tweets = tweets,
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
      kindleQuotedTweets = new PGKindleQuotedTweets[IO],
      kindleBooks = new PGKindleBooks[IO],
    )
    useCase(testUser.id).unsafeRunSync()

    assertEquals(tweets.newTweets.isEmpty, true)
  }

}
