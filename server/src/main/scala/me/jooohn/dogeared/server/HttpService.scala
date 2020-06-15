package me.jooohn.dogeared.server

import caliban.{CalibanError, GraphQLInterpreter, Http4sAdapter}
import cats.data.Kleisli
import cats.effect.Effect
import org.http4s.{Request, Response}
import org.http4s.implicits._
import org.http4s.server.Router
import zio._

object HttpService {

  def apply[F[_]: Effect, R](graphql: GraphQLInterpreter[R, CalibanError])(
      implicit R: Runtime[R]): Kleisli[F, Request[F], Response[F]] =
    Router(
      "/graphql" -> Http4sAdapter.makeHttpServiceF[F, CalibanError](
        graphql.asInstanceOf[GraphQLInterpreter[Any, CalibanError]])
    ).orNotFound

}
