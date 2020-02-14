package me.jooohn.dogeared.domain

import java.net.URL

case class KindleBook(
    id: KindleBookId,
    title: String,
    url: URL,
    authors: List[KindleBookAuthorName]
) {

  // TODO
  def slug: String = title

}
