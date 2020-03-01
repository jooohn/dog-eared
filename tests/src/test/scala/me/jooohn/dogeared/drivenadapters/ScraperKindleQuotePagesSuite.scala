package me.jooohn.dogeared.drivenadapters

import java.net.URL

import me.jooohn.dogeared.drivenports.{KindleQuotePage, KindleQuotePageResolutionError}
import org.http4s.Uri

import scala.io.Source
import cats.implicits._

class ScraperKindleQuotePagesSuite extends munit.FunSuite {

  test("should extract KindleQuotedPage data from html") {
    val uri = Uri.unsafeFromString(
      "https://read.amazon.com/kp/kshare?asin=B004OC071W&id=mbTfEuLgRI6x_6rkIJO4FA&reshareId=DG7X5MJE56VHQDAJPWJC&reshareChannel=system")
    val html = Source.fromResource("quote-page-sample.html").getLines().mkString("\n")
    val result = ScraperKindleQuotePages.extractKindleQuotePage(uri, html)
    assertEquals(
      result,
      KindleQuotePage(
        bookId = "B004OC071W",
        bookTitle =
          "The Practice of Adaptive Leadership: Tools and Tactics for Changing Your Organization and the World",
        bookAuthors = List("Ronald A. Heifetz", "Marty Linsky", "Alexander Grashow"),
        bookURL = new URL("https://www.amazon.com/dp/B004OC071W"),
        quotePageURL = new URL(
          "https://read.amazon.com/kp/kshare?asin=B004OC071W&id=mbTfEuLgRI6x_6rkIJO4FA&reshareId=DG7X5MJE56VHQDAJPWJC&reshareChannel=system"),
        quoteBody =
          "Adaptive leadership is an iterative process involving three key activities: (1) observing events and patterns around you; (2) interpreting what you are observing (developing multiple hypotheses about what is really going on); and (3) designing interventions based on the observations and interpretations to address the adaptive challenge you have identified."
      ).asRight[KindleQuotePageResolutionError],
    )
  }

}
