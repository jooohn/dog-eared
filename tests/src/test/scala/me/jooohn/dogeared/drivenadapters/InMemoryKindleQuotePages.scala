package me.jooohn.dogeared.drivenadapters

import cats.Monad
import me.jooohn.dogeared.domain.AmazonRedirectorURL
import me.jooohn.dogeared.drivenports.{
  InvalidRedirectLocation,
  KindleQuotePage,
  KindleQuotePageResolutionError,
  KindleQuotePages
}

class InMemoryKindleQuotePages[F[_]: Monad](map: Map[AmazonRedirectorURL, KindleQuotePage] = Map.empty)
    extends KindleQuotePages[F] {
  override def resolveManyByURLs(urls: List[AmazonRedirectorURL])
    : F[Map[AmazonRedirectorURL, Either[KindleQuotePageResolutionError, KindleQuotePage]]] = {
    Monad[F].pure(urls.map(url => url -> map.get(url).toRight(InvalidRedirectLocation(url.toString))).toMap)
  }
}
