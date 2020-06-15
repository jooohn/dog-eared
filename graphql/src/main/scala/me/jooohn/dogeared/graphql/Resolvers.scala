package me.jooohn.dogeared.graphql

import cats.MonadError
import cats.syntax.all._
import me.jooohn.dogeared.domain.{KindleBook, KindleQuotedTweet, TwitterUser}
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}

class Resolvers[F[_]: MonadError[*[_], Throwable]](
    twitterUserQueries: TwitterUserQueries[F],
    kindleQuotedTweetQueries: KindleQuotedTweetQueries[F],
    kindleBookQueries: KindleBookQueries[F],
) {
  val monadError: MonadError[F, Throwable] = MonadError[F, Throwable]

  def userById(id: Id): F[Option[User[F]]] = twitterUserQueries.resolve(id.value).map(_.map(_.toUser))
  def bookById(id: Id): F[Option[Book[F]]] = kindleBookQueries.resolve(id.value).map(_.map(_.toBook))

  lazy val queries: Queries[F] = Queries(
    user = userById,
    book = bookById,
  )

  implicit class TwitterUserOps(twitterUser: TwitterUser) {

    def toUser: User[F] = User(
      id = Id(twitterUser.id),
      username = Id(twitterUser.username),
      books = kindleBookQueries.resolveByUserId(twitterUser.id).map(_.map(_.toBook)),
      quotes = kindleQuotedTweetQueries.resolveByUserId(twitterUser.id).map(_.map(_.toQuote))
    )

  }

  implicit class KindleBookOps(kindleBook: KindleBook) {

    def toBook: Book[F] = Book(
      id = Id(kindleBook.id),
      title = kindleBook.title,
      url = kindleBook.url.toString,
      authors = kindleBook.authors,
      quotes = kindleQuotedTweetQueries.resolveByBookId(kindleBook.id).map(_.map(_.toQuote)),
      userQuotes =
        id => kindleQuotedTweetQueries.resolveByUserIdAndBookId(id.value, kindleBook.id).map(_.map(_.toQuote))
    )

  }

  implicit class KindleQuotedTweetOps(kindleQuotedTweet: KindleQuotedTweet) {

    def toQuote: Quote[F] = Quote(
      tweetId = Id(kindleQuotedTweet.tweetId),
      url = kindleQuotedTweet.quote.url.toString,
      body = kindleQuotedTweet.quote.body,
      user = twitterUserQueries
        .resolve(kindleQuotedTweet.twitterUserId)
        .flatMap(_.fold(monadError.raiseError[User[F]](
          new RuntimeException(s"user(${kindleQuotedTweet.twitterUserId}) could not be found.")))(_.toUser.pure[F])),
      book = kindleBookQueries
        .resolve(kindleQuotedTweet.bookId)
        .flatMap(_.fold(monadError.raiseError[Book[F]](
          new RuntimeException(s"book(${kindleQuotedTweet.bookId}) could not be found.")))(_.toBook.pure[F]))
    )

  }
}
