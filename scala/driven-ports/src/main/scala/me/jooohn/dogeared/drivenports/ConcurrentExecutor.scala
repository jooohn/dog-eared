package me.jooohn.dogeared.drivenports

import cats.effect.syntax.all._
import cats.effect.{Concurrent, Fiber}
import cats.implicits._

trait ConcurrentExecutor[F[_]] {

  def execute[A](fa: => F[A]): F[A]

  def async[A](fa: => F[A])(implicit F: Concurrent[F]): F[Fiber[F, A]] = execute(fa).start

  def all[A, B](as: List[A])(f: A => F[B])(implicit F: Concurrent[F]): F[List[B]] =
    as.traverse(a => async(f(a))) flatMap (_.traverse(_.join))

}
