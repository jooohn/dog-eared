package me.jooohn.dogeared.graphql

import caliban.schema.Schema

case class Queries[F[_]](
    user: Id => F[Option[User[F]]],
    book: Id => F[Option[Book[F]]],
)

case class User[F[_]](
    id: Id,
    username: Id,
    books: F[List[Book[F]]],
    quotes: F[List[Quote[F]]],
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
