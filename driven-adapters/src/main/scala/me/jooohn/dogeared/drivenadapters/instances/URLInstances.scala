package me.jooohn.dogeared.drivenadapters.instances

import java.net.URL

import cats.implicits._
import doobie.util.{Get, Put}

import scala.util.Try

trait URLInstances {

  implicit val urlGet: Get[URL] = Get[String].temap(string => Try(new URL(string)).toEither.leftMap(_.getMessage))
  implicit val urlPut: Put[URL] = Put[String].contramap(_.toString)

}
