package me.jooohn.dogeared.domain
import java.net.URL

case class KindleQuotedTweet(
    tweetId: TweetId,
    twitterUserId: TwitterUserId,
    quote: KindleQuote,
) {
  def bookId: KindleBookId = quote.bookId
}

case class KindleQuote(
    bookId: KindleBookId,
    url: URL,
    body: String,
)
