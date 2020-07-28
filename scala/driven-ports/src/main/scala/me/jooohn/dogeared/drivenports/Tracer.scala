package me.jooohn.dogeared.drivenports

trait Tracer[F[_]] {

  def traceId: String

  def span[A](name: String)(run: F[A]): F[A]

}
