package me.jooohn.dogeared.drivenports

import me.jooohn.dogeared.domain.{Tweet, TweetId, TwitterUser, TwitterUserId, TwitterUsername}

trait Twitter[F[_]] {

  def findUserTweets(twitterUserId: TwitterUserId, since: Option[TweetId]): F[List[Tweet]]

  def findUserAccount(twitterUserId: TwitterUserId): F[Option[TwitterUser]]
  def findUserAccountByName(twitterUserName: TwitterUsername): F[Option[TwitterUser]]

}
