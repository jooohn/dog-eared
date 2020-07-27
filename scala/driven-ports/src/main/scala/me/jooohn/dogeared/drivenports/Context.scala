package me.jooohn.dogeared.drivenports

import cats.data.Kleisli
import cats.{Applicative, Monad}

case class Context[F[_]](
    logger: Logger,
    tracer: Tracer[F],
) {

  def withAttributes(mapping: (String, Any)*): Context[F] = copy(logger = logger.withContext(mapping: _*))

}
