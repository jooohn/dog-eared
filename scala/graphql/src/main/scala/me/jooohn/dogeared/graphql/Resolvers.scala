package me.jooohn.dogeared.graphql

import caliban.CalibanError
import me.jooohn.dogeared.domain.{KindleBook, KindleQuotedTweet, TwitterUser}
import me.jooohn.dogeared.drivenports.{KindleBookQueries, KindleQuotedTweetQueries, TwitterUserQueries}
import me.jooohn.dogeared.usecases.{
  EnsureTwitterUserExistence,
  ImportKindleBookQuotesForUser,
  ImportUser,
  StartImportKindleBookQuotesForUser
}
import zio.{RIO, ZIO}

case class Resolvers(
    twitterUserQueries: TwitterUserQueries[Effect],
    kindleQuotedTweetQueries: KindleQuotedTweetQueries[Effect],
    kindleBookQueries: KindleBookQueries[Effect],
    importUser: ImportUser[Effect],
    importKindleBookQuotesForUser: ImportKindleBookQuotesForUser[Effect],
    startImportKindleBookQuotesForUser: StartImportKindleBookQuotesForUser[Effect],
) extends ResolversOps {
  import GraphQLContextRepository._

  val queries: Queries[EffectWithEnv] = Queries(
    user = id => twitterUserQueries.resolveByUsername(id.asTwitterUsername).map(_.map(_.toUser)),
    book = id => kindleBookQueries.resolve(id.value).map(_.map(_.toBook)),
    users = () => twitterUserQueries.resolveAll.map(_.map(_.toUser)),
  )

  val mutations: Mutations[EffectWithEnv] = Mutations(
    importUser = request => importUser(request.identity.value).handleLeft.map(Id.apply),
    importKindleBookQuotes = request =>
      importKindleBookQuotesForUser(
        twitterUserId = request.twitterUserId.asTwitterUserId,
        importOption = request.toImportOption,
      ).handleLeft,
    startImportKindleBookQuotes = request =>
      for {
        graphQLContext <- getGraphQLContext
        jobId <- startImportKindleBookQuotesForUser(
          twitterUserId = request.twitterUserId.asTwitterUserId,
          importOption = request.toImportOption,
        )(graphQLContext.toContext)
      } yield Id(jobId)
  )

  implicit class TwitterUserOps(twitterUser: TwitterUser) {

    def toUser: User[EffectWithEnv] = User(
      id = Id(twitterUser.id),
      username = Id(twitterUser.username),
      books = kindleBookQueries.resolveByUserId(twitterUser.id).map(_.map(_.toBook)),
      quotes = kindleQuotedTweetQueries.resolveByUserId(twitterUser.id).map(_.map(_.toQuote)),
      bookQuotes =
        bookId => kindleQuotedTweetQueries.resolveByUserIdAndBookId(twitterUser.id, bookId.value).map(_.map(_.toQuote))
    )

  }

  implicit class KindleBookOps(kindleBook: KindleBook) {

    def toBook: Book[EffectWithEnv] = Book(
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

    def toQuote: Quote[EffectWithEnv] = Quote(
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

trait ResolversOps {
  type EffectWithEnv[A] = RIO[Env, A]

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

  implicit class EffectEitherOps[A, B](fab: EffectWithEnv[Either[A, B]]) {

    def handleLeft(implicit T: ToCalibanError[A]): EffectWithEnv[B] = fab flatMap {
      case Left(a)  => T.toCalibanError(a).raiseError
      case Right(b) => b.pure
    }

  }

  implicit class AnyOps[A](a: A) {
    def pure: EffectWithEnv[A] = RIO(a)
  }

  implicit class ThrowableOps[R](t: Throwable) {
    def raiseError[A]: EffectWithEnv[A] = ZIO.fail(t)
  }
}
