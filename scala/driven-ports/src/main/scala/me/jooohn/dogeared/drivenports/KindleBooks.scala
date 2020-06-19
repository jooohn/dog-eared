package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.KindleBook

trait KindleBooks[F[_]] {

  def storeMany(kindleBooks: List[KindleBook]): F[Unit]

}
