package me.jooohn.dogeared.drivenports

trait ConcurrentIO[F[_]] {

  def apply[A](fa: => F[A]): F[A]

  def all[A, B](as: List[A])(f: A => F[B]): F[List[B]]

}
