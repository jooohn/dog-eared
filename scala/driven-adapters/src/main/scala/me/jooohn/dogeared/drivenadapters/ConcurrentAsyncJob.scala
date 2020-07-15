package me.jooohn.dogeared.drivenadapters

import java.util.UUID

import cats.effect.{ConcurrentEffect, Resource}
import cats.syntax.all._
import me.jooohn.dogeared.drivenports.Job.Id
import me.jooohn.dogeared.drivenports._

case class ConcurrentAsyncJob[F[_]: ConcurrentEffect](concurrentExecutor: ConcurrentExecutor[F]) extends AsyncJob[F] {
  override def later(job: F[JobResult])(implicit ctx: Context): F[Id] = {
    val jobId = UUID.randomUUID()
    val jobContext = ctx.withAttributes("jobId" -> jobId)
    concurrentExecutor.async(for {
      _ <- jobContext.logger.info("job started")
      result <- job.attempt
      _ <- result match {
        case Left(e)                  => jobContext.logger.error(e)("job failed")
        case Right(JobError(message)) => jobContext.logger.error(s"job error: ${message}")
        case Right(JobSuccess)        => jobContext.logger.info("job successfully finished")
      }
    } yield ()) *> jobContext.logger.info("job enqueued") as jobId.toString
  }
}

object ConcurrentAsyncJob {
  def fixedConcurrency[F[_]: ConcurrentEffect](concurrency: Int): Resource[F, AsyncJob[F]] =
    FixedConcurrentExecutor.resource[F](concurrency).map(ConcurrentAsyncJob[F])
}
