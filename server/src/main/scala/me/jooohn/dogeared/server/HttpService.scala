package me.jooohn.dogeared.server

import cats.implicits._
import caliban.{CalibanError, GraphQLInterpreter, Http4sAdapter}
import cats.{Applicative, Defer}
import cats.data.Kleisli
import cats.effect.{Effect, IO}
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.implicits._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router

class HttpService[F[_]: Effect, R](graphql: GraphQLInterpreter[R, CalibanError])(implicit R: zio.Runtime[R])
    extends Http4sDsl[F] {

  val healthCheckRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "healthcheck" => Ok("OK")
  }
  val graphQLRoutes: HttpRoutes[F] = Router(
    "/graphql" -> Http4sAdapter.makeHttpServiceF[F, CalibanError](
      graphql.asInstanceOf[GraphQLInterpreter[Any, CalibanError]])
  )

  val routes: Kleisli[F, Request[F], Response[F]] = (healthCheckRoutes <+> graphQLRoutes).orNotFound

}

object HttpService {

  def apply[F[_]: Effect, R](graphql: GraphQLInterpreter[R, CalibanError])(
      implicit R: zio.Runtime[R]): Kleisli[F, Request[F], Response[F]] = new HttpService[F, R](graphql).routes

}
