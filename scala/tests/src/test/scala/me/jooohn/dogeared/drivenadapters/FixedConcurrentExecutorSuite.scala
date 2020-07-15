package me.jooohn.dogeared.drivenadapters

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, TimeUnit}

import cats.effect.concurrent.Semaphore
import cats.effect.{ConcurrentEffect, ContextShift, IO, Timer}
import cats.implicits._
import me.jooohn.dogeared.util.IOContext

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.util.Success

class FixedConcurrentExecutorSuite extends munit.FunSuite with IOContext {
  override def munitFixtures = List(ioContext)

  def newSubject(
      nConcurrency: Int)(implicit cs: ContextShift[IO], ce: ConcurrentEffect[IO]): IO[FixedConcurrentExecutor[IO]] =
    Semaphore[IO](nConcurrency) map { semaphore =>
      new FixedConcurrentExecutor[IO](semaphore)
    }

  def sleep(millis: Int)(implicit t: Timer[IO]): IO[Unit] = {
    IO.sleep(FiniteDuration(millis, TimeUnit.MILLISECONDS))
  }

  def now(implicit t: Timer[IO]): IO[Long] =
    t.clock.realTime(scala.concurrent.duration.MILLISECONDS)

  def measure[A](fa: IO[A])(implicit t: Timer[IO]): IO[(A, Long)] =
    for {
      start <- now
      a <- fa
      end <- now
    } yield (a, end - start)

  test(".apply should execute IO with at most given concurrency") {
    val context = ioContext()
    implicit val timer = context.ioTimer
    implicit val cs = context.ioContextShift
    val future = FixedConcurrentExecutor
      .resource[IO](1)
      .use { executor =>
        for {
          fibers <- List(1, 2, 3).traverse(n => executor.execute(timer.sleep(0.5.second) *> IO(n)).start)
          results <- fibers.traverse(_.join)
        } yield results
      }
      .unsafeToFuture()

    assertEquals(future.value, None)

    context.tick(1.second)
    assertEquals(future.value, None)

    context.tick(1.second)
    assertEquals(future.value, Some(Success(List(1, 2, 3))))
  }

  test(".async should execute IO with at most given concurrency, but returns immediately") {
    val context = ioContext()
    implicit val timer = context.ioTimer
    implicit val cs = context.ioContextShift
    val counter = new AtomicInteger()
    val future = FixedConcurrentExecutor
      .resource[IO](1)
      .use { executor =>
        List(1, 2, 3).traverse(_ => executor.async(timer.sleep(0.5.second) *> IO(counter.addAndGet(1)))) as ()
      }
      .unsafeToFuture()
    context.tick(1.second)
    assertEquals(future.value, Some(Success(())))
    assertEquals(counter.get(), 2)

    context.tick(1.second)
    assertEquals(counter.get(), 3)
  }

  test(".all should execute given IO sequentially when concurrency = 1") {
    val context = ioContext()
    implicit val timer = context.ioTimer
    implicit val cs = context.ioContextShift

    val future = FixedConcurrentExecutor
      .resource[IO](1)
      .use { executor =>
        executor.all(List(1, 2, 3))(index => sleep(500) *> IO(index))
      }
      .unsafeToFuture()

    assertEquals(future.value, None)

    context.tick(1.second)
    assertEquals(future.value, None)

    context.tick(1.second)
    assertEquals(future.value, Some(Success(List(1, 2, 3))))
  }

  test(".all should execute given IO concurrently when concurrency > 1") {
    val context = ioContext()
    implicit val timer = context.ioTimer
    implicit val cs = context.ioContextShift

    val future = FixedConcurrentExecutor
      .resource[IO](3)
      .use { executor =>
        executor.all(List(1, 2, 3))(index => sleep(500) *> IO(index))
      }
      .unsafeToFuture()

    assertEquals(future.value, None)

    context.tick(1.second)
    assertEquals(future.value, Some(Success(List(1, 2, 3))))
  }

}
