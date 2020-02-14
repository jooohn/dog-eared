package me.jooohn.dogeared.domain

import java.net.URL

case class Tweet(id: TweetId, userId: TwitterUserId, body: String) {

  lazy val linkToAmazon: Option[URL] = Some(new URL("https://example.com"))

  def kindleQuoted(kindleQuote: KindleQuote): KindleQuotedTweet = KindleQuotedTweet(
    tweetId = id,
    twitterUserId = userId,
    quote = kindleQuote,
  )

}
