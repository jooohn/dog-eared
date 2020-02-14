package me.jooohn.dogeared

package object domain {

  type StringId[A] = String

  type KindleBookId = StringId[KindleBook]
  type KindleBookAuthorName = StringId[KindleBookAuthor]
  type KindleBookQuoteId = StringId[KindleQuotedTweet]
  type TwitterUserId = StringId[TwitterUser]
  type TwitterUsername = StringId[TwitterUser]

  type TweetId = StringId[Tweet]

}
