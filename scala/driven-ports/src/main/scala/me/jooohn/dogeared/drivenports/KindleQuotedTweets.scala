package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.KindleQuotedTweet

trait KindleQuotedTweets[F[_]] {

  def storeMany(kindleQuotedTweets: List[KindleQuotedTweet]): F[Unit]

}
