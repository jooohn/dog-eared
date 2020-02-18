package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TweetId, TwitterUserId}

case class ProcessedTweet(
    twitterUserId: TwitterUserId,
    lastProcessedTweetId: TweetId,
)

trait ProcessedTweets[F[_]] {

  def resolveByUserId(twitterUserId: TwitterUserId): F[Option[ProcessedTweet]]

  def recordLastProcessedTweetId(twitterUserId: TwitterUserId, tweetId: TweetId): F[Unit]

}
