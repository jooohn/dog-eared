package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.data.{EitherT, NonEmptyList, ValidatedNel}
import cats.effect.{Sync, Timer}
import cats.implicits._
import me.jooohn.dogeared.domain.AmazonRedirectorURL
import me.jooohn.dogeared.drivenports._
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import org.http4s.client.Client
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Response, Status, Uri}

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class ScraperKindleQuotePages[F[_]: Sync](fetchInterval: FiniteDuration, client: Client[F])(implicit timer: Timer[F])
    extends KindleQuotePages[F] {

  override def resolveManyByURLs(urls: List[AmazonRedirectorURL])
    : F[Map[AmazonRedirectorURL, Either[KindleQuotePageResolutionError, KindleQuotePage]]] =
    urls.distinct.mapWithInterval(fetchKindleQuotePage)

  private def fetchKindleQuotePage(
      amazonRedirectorURL: AmazonRedirectorURL): F[Either[KindleQuotePageResolutionError, KindleQuotePage]] =
    (for {
      originalUri <- EitherT(resolveOriginalUri(amazonRedirectorURL))
      body <- EitherT.liftF(client.expect[String](originalUri))
      kindleQuotePage <- EitherT.fromEither[F](ScraperKindleQuotePages.extractKindleQuotePage(originalUri, body))
    } yield kindleQuotePage).value

  private def resolveOriginalUri(
      amazonRedirectorURL: AmazonRedirectorURL): F[Either[KindleQuotePageResolutionError, Uri]] = {
    val uri = Uri.unsafeFromString(amazonRedirectorURL.url.toString)
    (for {
      locationHeader <- EitherT(client.get(uri)(_.redirectLocation.pure[F]))
      targetUri <- EitherT.fromEither[F](locationHeader.valueAsTargetPageUri)
    } yield targetUri).value
  }

  implicit class UriOps(uri: Uri) {

    def isTargetPageUri: Boolean =
      uri.host.exists(_.value == "read.amazon.com") && uri.path == "/kp/kshare"

  }

  implicit class HeaderOps(header: Header) {

    def valueAsTargetPageUri: Either[KindleQuotePageResolutionError, Uri] =
      Uri.fromString(header.value).toOption.filter(_.isTargetPageUri).toRight(InvalidRedirectLocation(header.value))

  }

  implicit class ResponseOps(response: Response[F]) {

    def redirectLocation: Either[KindleQuotePageResolutionError, Header] =
      for {
        _ <- ensureMovedPermanently
        locationHeader <- findLocationHeader
      } yield locationHeader

    def ensureMovedPermanently: Either[KindleQuotePageResolutionError, Response[F]] =
      if (response.status == Status.MovedPermanently) Right(response)
      else Left(InvalidRedirectorResponse(response.status.code, response.headers.toList.map(_.toString())))

    def findLocationHeader: Either[KindleQuotePageResolutionError, Header] =
      response.headers
        .find(_.name == CaseInsensitiveString("Location"))
        .toRight(InvalidRedirectorResponse(response.status.code, response.headers.toList.map(_.toString())))

  }

  implicit class URLsOps(urls: List[AmazonRedirectorURL]) {

    def mapWithInterval[A](f: AmazonRedirectorURL => F[A]): F[Map[AmazonRedirectorURL, A]] =
      urls
        .traverse(url => (f(url) map (url -> _)) <* timer.sleep(fetchInterval))
        .map(_.toMap)

  }

}

object ScraperKindleQuotePages {

  def extractKindleQuotePage(uri: Uri, bodyHTML: String): Either[KindleQuotePageResolutionError, KindleQuotePage] = {
    val doc = JsoupBrowser().parseString(bodyHTML)
    (
      doc.quote,
      doc.bookTitle,
      doc.bookAuthors,
      uri.query.params.get("asin").toValid(NonEmptyList.of(s"asin not found in URL ${uri}")),
    ).mapN { (quote, bookTitle, bookAuthors, bookId) =>
        KindleQuotePage(
          bookId = bookId,
          bookTitle = bookTitle,
          bookAuthors = bookAuthors,
          bookURL = new URL(s"https://www.amazon.com/dp/${bookId}"),
          quotePageURL = new URL(uri.toString),
          quoteBody = quote,
        )
      }
      .toEither
      .leftMap(es => InvalidAttributes(es.toList))
  }

  private implicit class JSoupKindleQuotePageDocumentOps(doc: Browser#DocumentType) {
    import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
    import net.ruippeixotog.scalascraper.dsl.DSL._

    def textValidated(query: String): ValidatedNel[String, String] =
      Try(doc >> text(query)).fold(
        e => s"""query "${query}" does not exist.""".invalidNel,
        _.valid
      )

    def quote: ValidatedNel[String, String] =
      textValidated(".kp-quote")
        .map { text =>
          text.trim().stripPrefix("\"").stripSuffix("\"")
        }
        .ensure(NonEmptyList.of("quote body not found"))(_.nonEmpty)

    def bookTitle: ValidatedNel[String, String] =
      textValidated(".kp-title").ensure(NonEmptyList.of("book title not found"))(_.nonEmpty)

    def bookAuthors: ValidatedNel[String, List[String]] =
      textValidated(".kp-author")
        .map { text =>
          text
            .trim()
            .stripPrefix("by")
            .trim()
            .split(",")
            .toList
            .map(_.trim())
        }
        .ensure(NonEmptyList.of("authors not found"))(_.nonEmpty)
  }

}
