package me.jooohn.dogeared.graphql

import caliban.CalibanError
import me.jooohn.dogeared.domain.{KindleBook, KindleQuotedTweet, TwitterUser}
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}
import me.jooohn.dogeared.usecases.{EnsureTwitterUserExistence, ImportKindleBookQuotesForUser, ImportUser}
import zio.{Has, RIO, ZIO}

case class Resolvers[R <: Has[_]](
    twitterUserQueries: TwitterUserQueries[RIO[R, *]],
    kindleQuotedTweetQueries: KindleQuotedTweetQueries[RIO[R, *]],
    kindleBookQueries: KindleBookQueries[RIO[R, *]],
    importUser: ImportUser[RIO[R, *]],
    importKindleBookQuotesForUser: ImportKindleBookQuotesForUser[RIO[R, *]]
) extends ResolversOps[R] {

  val queries: Queries[Effect] = Queries(
    user = id => twitterUserQueries.resolveByUsername(id.asTwitterUsername).map(_.map(_.toUser)),
    book = id => kindleBookQueries.resolve(id.value).map(_.map(_.toBook)),
    users = () => twitterUserQueries.resolveAll.map(_.map(_.toUser)),
  )

  val mutations: Mutations[Effect] = Mutations(
    importUser = request => importUser(request.identity.value).handleLeft.map(Id.apply),
    importKindleBookQuotes = request => importKindleBookQuotesForUser(request.twitterUserId.asTwitterUserId).handleLeft
  )

  implicit class TwitterUserOps(twitterUser: TwitterUser) {

    def toUser: User[Effect] = User(
      id = Id(twitterUser.id),
      username = Id(twitterUser.username),
      books = kindleBookQueries.resolveByUserId(twitterUser.id).map(_.map(_.toBook)),
      quotes = kindleQuotedTweetQueries.resolveByUserId(twitterUser.id).map(_.map(_.toQuote)),
      bookQuotes =
        bookId => kindleQuotedTweetQueries.resolveByUserIdAndBookId(twitterUser.id, bookId.value).map(_.map(_.toQuote))
    )

  }

  implicit class KindleBookOps(kindleBook: KindleBook) {

    def toBook: Book[Effect] = Book(
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

    def toQuote: Quote[Effect] = Quote(
      tweetId = Id(kindleQuotedTweet.tweetId),
      url = kindleQuotedTweet.quote.url.toString,
      body = kindleQuotedTweet.quote.body,
      user = twitterUserQueries.resolve(kindleQuotedTweet.twitterUserId) flatMap {
        case Some(twitterUser) => twitterUser.toUser.pure
        case None              => new RuntimeException(s"user(${kindleQuotedTweet.twitterUserId}) doesn't exist.").raiseError
      },
      book = kindleBookQueries.resolve(kindleQuotedTweet.bookId) flatMap {
        case Some(kindleBook) => kindleBook.toBook.pure
        case None =>
          new RuntimeException(s"book(${kindleQuotedTweet.bookId}) doesn't exist.").raiseError
      },
    )

  }
}

trait ResolversOps[R] {
  type Effect[A] = RIO[EnvWith[R], A]

  trait ToCalibanError[A] {

    def toCalibanError(value: A): CalibanError

  }

  implicit val importKindleBookQuotesForUserErrorToCalibanError: ToCalibanError[ImportKindleBookQuotesForUser.Error] = {
    case EnsureTwitterUserExistence.UserNotFound(id) =>
      CalibanError.ExecutionError(msg = s"user ${id} not found")
  }

  implicit val importUserErrorToCalibanErro: ToCalibanError[ImportUser.Error] = {
    case ImportUser.UserNotFound(identity) =>
      CalibanError.ExecutionError(msg = s"user ${identity} not found")
  }

  implicit class EffectEitherOps[A, B](fab: Effect[Either[A, B]]) {

    def handleLeft(implicit T: ToCalibanError[A]): Effect[B] = fab flatMap {
      case Left(a)  => T.toCalibanError(a).raiseError
      case Right(b) => b.pure
    }

  }

  implicit class AnyOps[A](a: A) {
    def pure: Effect[A] = RIO(a)
  }

  implicit class ThrowableOps[R](t: Throwable) {
    def raiseError[A]: Effect[A] = ZIO.fail(t)
  }
}
