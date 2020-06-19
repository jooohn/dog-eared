package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{TweetId, TwitterUserId}

case class ProcessedTweet(
    twitterUserId: TwitterUserId,
    latestProcessedTweetId: TweetId,
)

trait ProcessedTweets[F[_]] {

  def resolveByUserId(twitterUserId: TwitterUserId): F[Option[ProcessedTweet]]

  def recordLatestProcessedTweetId(twitterUserId: TwitterUserId, tweetId: TweetId): F[Unit]

}
