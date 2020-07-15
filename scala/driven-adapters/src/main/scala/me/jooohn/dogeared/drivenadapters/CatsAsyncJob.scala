package me.jooohn.dogeared.drivenadapters

import java.util.UUID

import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.syntax.all._
import cats.effect.{Concurrent, Resource}
import cats.syntax.all._
import me.jooohn.dogeared.drivenports.Job.Id
import me.jooohn.dogeared.drivenports.{AsyncJob, Context, Job, JobResult}

import scala.collection.immutable.Queue

object CatsAsyncJob {
  private[this] class CatsAsyncJob[F[_]: Concurrent](
      val cancel: F[Unit],
      queueSemaphore: Semaphore[F],
      queue: Ref[F, Queue[Job[F]]]
  ) extends AsyncJob[F] {

    override def later(job: F[JobResult])(implicit ctx: Context): F[Id] = {
      val jobId = UUID.randomUUID()
      val updatedContext = ctx.withAttributes("jobId" -> jobId)
      for {
        _ <- queue.update(
          _.enqueue(
            Job(
              id = jobId.toString,
              run = job,
              context = updatedContext
            )))
        _ <- updatedContext.logger.info("job enqueued")
        _ <- queueSemaphore.release
      } yield jobId.toString
    }
  }

  def resource[F[_]: Concurrent](concurrency: Int): Resource[F, AsyncJob[F]] =
    Resource.make[F, CatsAsyncJob[F]] {
      for {
        queue <- Ref.of(Queue.empty[Job[F]])
        queueSemaphore <- Semaphore(0)
        concurrencySemaphore <- Semaphore(concurrency)
        runFiber <- {
          def run: F[Unit] =
            for {
              _ <- queueSemaphore.acquire
              job <- queue.modify(_.dequeue.swap)
              _ <- concurrencySemaphore.acquire
              _ <- (job.run *> job.context.logger.info("job finished")).guarantee(concurrencySemaphore.release).start
              _ <- job.context.logger.info("job started")
              _ <- run
            } yield ()
          run.start
        }
      } yield
        new CatsAsyncJob(
          cancel = runFiber.cancel,
          queueSemaphore = queueSemaphore,
          queue = queue,
        )
    } { asyncJob =>
      asyncJob.cancel
    }
}
