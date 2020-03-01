package me.jooohn.dogeared.drivenports

import java.net.URL

import me.jooohn.dogeared.domain.{AmazonRedirectorURL, KindleBook, KindleBookAuthorName, KindleBookId, KindleQuote}

case class KindleQuotePage(
    bookId: KindleBookId,
    bookTitle: String,
    bookAuthors: List[KindleBookAuthorName],
    bookURL: URL,
    quotePageURL: URL,
    quoteBody: String
) {

  def book: KindleBook = KindleBook(
    id = bookId,
    title = bookTitle,
    url = bookURL,
    authors = bookAuthors,
  )

  def quote: KindleQuote = KindleQuote(
    bookId = bookId,
    url = quotePageURL,
    body = quoteBody,
  )

}

sealed trait KindleQuotePageResolutionError
case class InvalidRedirectorResponse(status: Int, headers: List[String]) extends KindleQuotePageResolutionError
case class InvalidRedirectLocation(url: String) extends KindleQuotePageResolutionError
case class UnintendedOriginalUrl(url: String) extends KindleQuotePageResolutionError
case class InvalidAttributes(reasons: List[String]) extends KindleQuotePageResolutionError

trait KindleQuotePages[F[_]] {

  def resolveManyByURLs(urls: List[AmazonRedirectorURL])
    : F[Map[AmazonRedirectorURL, Either[KindleQuotePageResolutionError, KindleQuotePage]]]

}
