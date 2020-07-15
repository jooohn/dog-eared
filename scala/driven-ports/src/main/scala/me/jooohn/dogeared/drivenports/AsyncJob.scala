package me.jooohn.dogeared.drivenports

import cats.effect.Fiber

case class Job[F[_]](
    id: Job.Id,
    run: F[JobResult],
    context: Context,
)
object Job {
  type Id = String
}

sealed trait JobResult {

  def fold[A](onError: String => A, onSuccess: => A): A = this match {
    case JobSuccess        => onSuccess
    case JobError(message) => onError(message)
  }

}
case object JobSuccess extends JobResult
case class JobError(message: String) extends JobResult

trait AsyncJob[F[_]] {

  def later(job: F[JobResult])(implicit ctx: Context): F[Job.Id]

}
