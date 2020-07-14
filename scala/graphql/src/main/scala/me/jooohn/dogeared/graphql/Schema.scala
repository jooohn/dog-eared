package me.jooohn.dogeared.graphql

import caliban.schema.Annotations.GQLDirective
import me.jooohn.dogeared.graphql.Directives.internal
import me.jooohn.dogeared.usecases.ImportUser

case class Queries[F[_]](
    user: Id => F[Option[User[F]]],
    book: Id => F[Option[Book[F]]],
    @GQLDirective(internal) users: () => F[List[User[F]]],
)

case class Mutations[F[_]](
    @GQLDirective(internal) importUser: ImportUserRequest => F[Id],
    @GQLDirective(internal) importKindleBookQuotes: ImportKindleBookQuotesRequest => F[Unit]
)

case class ImportUserRequest(
    identity: Id,
)

case class ImportKindleBookQuotesRequest(
    twitterUserId: Id,
    forceUpdate: Option[Boolean],
)

case class User[F[_]](
    id: Id,
    username: Id,
    books: F[List[Book[F]]],
    quotes: F[List[Quote[F]]],
    bookQuotes: Id => F[List[Quote[F]]],
)

case class Book[F[_]](
    id: Id,
    title: String,
    url: String,
    authors: List[String],
    quotes: F[List[Quote[F]]],
    userQuotes: Id => F[List[Quote[F]]],
)

case class Quote[F[_]](
    tweetId: Id,
    url: String,
    body: String,
    user: F[User[F]],
    book: F[Book[F]],
)
