package me.jooohn.dogeared.drivenadapters.instances

import doobie.util.Write
import me.jooohn.dogeared.drivenports.ProcessedTweet

trait ProcessedTweetInstances {

  implicit val processedTweetWrite: Write[ProcessedTweet] =
    Write[(String, String)].contramap(pt => (pt.twitterUserId, pt.lastProcessedTweetId))

}
