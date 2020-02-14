package me.jooohn.dogeared.drivenadapters.instances

import java.net.URL

import doobie.util.Write
import doobie.postgres.implicits._
import me.jooohn.dogeared.domain.KindleBook

trait KindleBookInstances {
  import url._

  implicit val kindleBookWrite: Write[KindleBook] =
    Write[(String, String, URL, List[String], String)].contramap(
      kb =>
        (
          kb.id,
          kb.title,
          kb.url,
          kb.authors,
          kb.slug,
      ))

}
