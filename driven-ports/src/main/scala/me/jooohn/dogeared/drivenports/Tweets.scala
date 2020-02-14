package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{Tweet, TwitterUserId}

trait Tweets[F[_]] {

  def processNewTweets(twitterUserId: TwitterUserId)(f: List[Tweet] => F[Unit]): F[Unit]

}
