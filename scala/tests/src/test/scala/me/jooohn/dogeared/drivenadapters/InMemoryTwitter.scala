package me.jooohn.dogeared.drivenadapters

import cats.Monad
import me.jooohn.dogeared.domain.{Tweet, TweetId, TwitterUser, TwitterUserId}
import me.jooohn.dogeared.drivenports.Twitter

case class InMemoryTwitter[F[_]: Monad](
    tweets: List[Tweet] = Nil,
    twitterUsers: List[TwitterUser] = Nil
) extends Twitter[F] {
  override def findUserTweets(twitterUserId: TwitterUserId, since: Option[TweetId]): F[List[Tweet]] =
    Monad[F].pure(tweets.filter(_.userId == twitterUserId).takeWhile(tweet => !since.contains(tweet.id)))

  override def findUserAccount(twitterUserId: TwitterUserId): F[Option[TwitterUser]] =
    Monad[F].pure(twitterUsers.find(_.id == twitterUserId))
}
