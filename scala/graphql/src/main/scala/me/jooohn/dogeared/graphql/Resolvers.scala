package me.jooohn.dogeared.graphql

import cats.MonadError
import cats.effect.IO
import cats.syntax.all._
import me.jooohn.dogeared.domain.{KindleBook, KindleQuotedTweet, TwitterUser}
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}

class Resolvers(
    twitterUserQueries: TwitterUserQueries[IO],
    kindleQuotedTweetQueries: KindleQuotedTweetQueries[IO],
    kindleBookQueries: KindleBookQueries[IO],
) {
  def userById(id: Id): IO[Option[User]] = twitterUserQueries.resolveByUsername(id.value).map(_.map(_.toUser))
  def bookById(id: Id): IO[Option[Book]] = kindleBookQueries.resolve(id.value).map(_.map(_.toBook))

  lazy val queries: Queries = Queries(
    user = userById,
    book = bookById,
  )

  implicit class TwitterUserOps(twitterUser: TwitterUser) {

    def toUser: User = User(
      id = Id(twitterUser.id),
      username = Id(twitterUser.username),
      books = kindleBookQueries.resolveByUserId(twitterUser.id).map(_.map(_.toBook)),
      quotes = kindleQuotedTweetQueries.resolveByUserId(twitterUser.id).map(_.map(_.toQuote)),
      bookQuotes =
        bookId => kindleQuotedTweetQueries.resolveByUserIdAndBookId(twitterUser.id, bookId.value).map(_.map(_.toQuote))
    )

  }

  implicit class KindleBookOps(kindleBook: KindleBook) {

    def toBook: Book = Book(
      id = Id(kindleBook.id),
      title = kindleBook.title,
      url = kindleBook.url.toString,
      authors = kindleBook.authors,
      quotes = kindleQuotedTweetQueries.resolveByBookId(kindleBook.id).map(_.map(_.toQuote)),
      userQuotes =
        userId => kindleQuotedTweetQueries.resolveByUserIdAndBookId(userId.value, kindleBook.id).map(_.map(_.toQuote))
    )

  }

  implicit class KindleQuotedTweetOps(kindleQuotedTweet: KindleQuotedTweet) {

    def toQuote: Quote = Quote(
      tweetId = Id(kindleQuotedTweet.tweetId),
      url = kindleQuotedTweet.quote.url.toString,
      body = kindleQuotedTweet.quote.body,
      user = twitterUserQueries
        .resolve(kindleQuotedTweet.twitterUserId)
        .flatMap(_.fold(IO.raiseError[User](
          new RuntimeException(s"user(${kindleQuotedTweet.twitterUserId}) could not be found.")))(_.toUser.pure[IO])),
      book = kindleBookQueries
        .resolve(kindleQuotedTweet.bookId)
        .flatMap(
          _.fold(IO.raiseError[Book](new RuntimeException(s"book(${kindleQuotedTweet.bookId}) could not be found.")))(
            _.toBook.pure[IO]))
    )

  }
}
