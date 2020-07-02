package me.jooohn.dogeared.graphql

import caliban.schema.Annotations.GQLDirective
import cats.effect.IO
import Directives.internal

case class Queries(
    user: Id => IO[Option[User]],
    book: Id => IO[Option[Book]],
)

case class Mutations(
    @GQLDirective(internal) importKindleBookQuotes: ImportKindleBookQuotesRequest => IO[Unit]
)

case class ImportKindleBookQuotesRequest(
    twitterUserId: Id,
)

case class User(
    id: Id,
    username: Id,
    books: IO[List[Book]],
    quotes: IO[List[Quote]],
    bookQuotes: Id => IO[List[Quote]],
)

case class Book(
    id: Id,
    title: String,
    url: String,
    authors: List[String],
    quotes: IO[List[Quote]],
    userQuotes: Id => IO[List[Quote]],
)

case class Quote(
    tweetId: Id,
    url: String,
    body: String,
    user: IO[User],
    book: IO[Book],
)
