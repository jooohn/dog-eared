package me.jooohn.dogeared.graphql

import caliban.CalibanError
import cats.effect.IO
import cats.syntax.all._
import me.jooohn.dogeared.domain.{KindleBook, KindleQuotedTweet, TwitterUser}
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}
import me.jooohn.dogeared.usecases.{EnsureTwitterUserExistence, ImportKindleBookQuotesForUser}

case class Resolvers(
    twitterUserQueries: TwitterUserQueries[IO],
    kindleQuotedTweetQueries: KindleQuotedTweetQueries[IO],
    kindleBookQueries: KindleBookQueries[IO],
    importKindleBookQuotesForUser: ImportKindleBookQuotesForUser[IO]
) extends ResolversOps {

  val queries: Queries = Queries(
    user = id => twitterUserQueries.resolveByUsername(id.asTwitterUsername).map(_.map(_.toUser)),
    book = id => kindleBookQueries.resolve(id.value).map(_.map(_.toBook)),
  )

  val mutations: Mutations = Mutations(
    importKindleBookQuotes = request => importKindleBookQuotesForUser(request.twitterUserId.asTwitterUserId).handleLeft
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

trait ResolversOps {
  trait ToCalibanError[A] {

    def toCalibanError(value: A): CalibanError

  }

  implicit class IOEitherOps[A, B](io: IO[Either[A, B]]) {

    def handleLeft(implicit T: ToCalibanError[A]): IO[B] = io flatMap {
      case Left(a)  => IO.raiseError(T.toCalibanError(a))
      case Right(b) => IO.pure(b)
    }

  }

  implicit val importKindleBookQuotesForUserErrorToCalibanError: ToCalibanError[ImportKindleBookQuotesForUser.Error] = {
    case EnsureTwitterUserExistence.UserNotFound(id) =>
      CalibanError.ValidationError(
        msg = "USER_NOT_FOUND",
        explanatoryText = s"twitter user ${id} was not found"
      )
  }

}
