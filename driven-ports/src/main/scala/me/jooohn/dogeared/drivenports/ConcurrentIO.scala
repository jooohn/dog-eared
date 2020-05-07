package me.jooohn.dogeared.drivenports

trait ConcurrentIO[F[_]] {

  def all[A, B](as: List[A])(f: A => F[B]): F[List[B]]

}
