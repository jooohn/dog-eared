package me.jooohn.dogeared.graphql

import caliban.interop.cats.implicits._
import cats.effect.{ExitCode, IO, IOApp}
import me.jooohn.dogeared.app.ProductionApp
import zio.Runtime

//object Main extends IOApp {
//  implicit val runtime = Runtime.default
//
//  val query =
//    """
//      |query A {
//      |  user(value: "92690919") {
//      |    id
//      |    username
//      |    books {
//      |      title
//      |      userQuotes(value: "92690919") {
//      |        body
//      |      }
//      |    }
//      |  }
//      |}
//      |""".stripMargin
//
//  override def run(args: List[String]): IO[ExitCode] = {
//    ProductionApp { module =>
//      for {
//        interpreter <- GraphQL.interpreter(
//          twitterUserQueries = module.twitterUserQueries,
//          kindleBookQueries = module.kindleBookQueries,
//          kindleQuotedTweetQueries = module.kindleQuotedTweetQueries,
//        )
//        result <- interpreter.executeAsync[IO](query)
//        _ <- IO(println(result))
//      } yield ExitCode.Success
//    }
//  }
//}
