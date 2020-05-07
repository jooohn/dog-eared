package me.jooohn.dogeared.drivenadapters

import cats.effect.{ConcurrentEffect, Fiber}
import cats.effect.concurrent.Semaphore
import cats.implicits._
import me.jooohn.dogeared.drivenports.ConcurrentIO

class FixedConcurrencyIO[F[_]: ConcurrentEffect](semaphore: Semaphore[F]) extends ConcurrentIO[F] {

  override def all[A, B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.traverse(a => ConcurrentEffect[F].start(bracket(f(a)))) flatMap { fibers =>
      fibers.traverse(_.join)
    }

  private def bracket[A](fa: => F[A]): F[A] =
    ConcurrentEffect[F].bracket {
      semaphore.acquire
    } { _ =>
      fa
    } { _ =>
      semaphore.release
    }

}
