package me.jooohn.dogeared.drivenports

trait Tracer[F[_]] {

  def span[A](name: String)(run: F[A]): F[A]

}
