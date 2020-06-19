package me.jooohn.dogeared.domain

import java.net.URL

case class Tweet(id: TweetId, userId: TwitterUserId, amazonRedirectorLinkURL: AmazonRedirectorURL) {

  def kindleQuoted(kindleQuote: KindleQuote): KindleQuotedTweet = KindleQuotedTweet(
    tweetId = id,
    twitterUserId = userId,
    quote = kindleQuote,
  )

}

case class AmazonRedirectorURL(url: URL) {
  require(url.getHost == AmazonRedirectorURL.host)
}
object AmazonRedirectorURL {

  val host = "a.co"

  def fromURL(url: URL): Option[AmazonRedirectorURL] =
    if (url.getHost == host) Some(AmazonRedirectorURL(url))
    else None

}
