package me.jooohn.dogeared.drivenports

import cats.effect.Sync

trait Logger {

  def info[F[_]: Sync](message: => String): F[Unit]
  def error[F[_]: Sync](message: => String): F[Unit]
  def error[F[_]: Sync](throwable: Throwable)(message: String = throwable.getMessage): F[Unit]

  def withContext(mapping: (String, Any)*): Logger

}
