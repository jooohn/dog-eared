package me.jooohn.dogeared.drivenadapters

import java.net.URL

import cats.Monad
import me.jooohn.dogeared.drivenports.{KindleQuotePage, KindleQuotePages}

class InMemoryKindleQuotePages[F[_]: Monad](map: Map[URL, KindleQuotePage] = Map.empty) extends KindleQuotePages[F] {
  override def resolveManyByURLs(urls: List[URL]): F[Map[URL, KindleQuotePage]] = {
    val targetURLs = urls.toSet
    Monad[F].pure(map.filter(entry => targetURLs(entry._1)))
  }
}
