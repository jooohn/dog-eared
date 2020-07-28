package me.jooohn.dogeared.drivenadapters

import cats.Monad
import cats.effect.concurrent.Semaphore
import cats.effect.{ConcurrentEffect, Resource}
import cats.implicits._
import me.jooohn.dogeared.drivenports.ConcurrentExecutor

case class FixedConcurrentExecutor[F[_]: ConcurrentEffect](semaphore: Semaphore[F]) extends ConcurrentExecutor[F] {

  override def execute[A](fa: => F[A]): F[A] =
    semaphore.withPermit(fa)

}
object FixedConcurrentExecutor {

  def resource[F[_]: ConcurrentEffect](concurrency: Int): Resource[F, ConcurrentExecutor[F]] =
    Resource.make(Semaphore[F](concurrency).map(FixedConcurrentExecutor[F]))(_ => Monad[F].unit)

}
