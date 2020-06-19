package me.jooohn.dogeared.drivenadapters.instances

import java.net.URL

import doobie.util.Write
import me.jooohn.dogeared.domain.KindleQuotedTweet

trait KindleQuotedTweetInstances {
  import url._

  implicit val kindleQuotedTweetWrite: Write[KindleQuotedTweet] =
    Write[(String, String, String, URL, String)].contramap(
      kqt =>
        (
          kqt.tweetId,
          kqt.twitterUserId,
          kqt.quote.bookId,
          kqt.quote.url,
          kqt.quote.body
      ))

}
