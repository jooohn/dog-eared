package me.jooohn.dogeared.drivenadapters

import cats.Monad
import me.jooohn.dogeared.domain.AmazonRedirectorURL
import me.jooohn.dogeared.drivenports.{KindleQuotePage, KindleQuotePages}

class InMemoryKindleQuotePages[F[_]: Monad](map: Map[AmazonRedirectorURL, KindleQuotePage] = Map.empty)
    extends KindleQuotePages[F] {
  override def resolveManyByURLs(urls: List[AmazonRedirectorURL]): F[Map[AmazonRedirectorURL, KindleQuotePage]] = {
    val targetURLs = urls.toSet
    Monad[F].pure(map.filter(entry => targetURLs(entry._1)))
  }
}
