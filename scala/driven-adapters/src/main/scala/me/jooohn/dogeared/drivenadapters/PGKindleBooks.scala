package me.jooohn.dogeared.drivenadapters

import cats.Monad
import cats.effect.Bracket
import cats.implicits._
import doobie.implicits._
import doobie.{Transactor, Update}
import me.jooohn.dogeared.domain.KindleBook
import me.jooohn.dogeared.drivenadapters.instances.kindleBook._
import me.jooohn.dogeared.drivenports.KindleBooks

class PGKindleBooks[F[_]: Monad: Transactor: Bracket[*[_], Throwable]] extends KindleBooks[F] {

  override def storeMany(kindleBooks: List[KindleBook]): F[Unit] = {
    val sql =
      """
        |INSERT INTO kindle_books
        |(id, title, url, authors, slug)
        |VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE
        |SET title    = EXCLUDED.title,
        |    url      = EXCLUDED.url,
        |    authors  = EXCLUDED.authors,
        |    slug     = EXCLUDED.slug
        |""".stripMargin
    Update[KindleBook](sql)
      .updateMany(kindleBooks)
      .transact[F](implicitly[Transactor[F]]) *> Monad[F].unit
  }

}
