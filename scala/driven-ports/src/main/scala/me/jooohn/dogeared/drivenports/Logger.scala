package me.jooohn.dogeared.drivenports

import cats.effect.Sync

trait Logger {

  def info[F[_]: Sync](message: => String): F[Unit]
  def error[F[_]: Sync](message: => String): F[Unit]
  def error[F[_]: Sync](throwable: Throwable): F[Unit]

  def withContext(mapping: (String, String)*): Logger

}
