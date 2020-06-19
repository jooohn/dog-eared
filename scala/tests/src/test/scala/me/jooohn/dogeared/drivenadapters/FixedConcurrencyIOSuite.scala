package me.jooohn.dogeared.drivenadapters

import java.util.concurrent.{Executors, TimeUnit}

import cats.effect.concurrent.Semaphore
import cats.effect.{ConcurrentEffect, IO}
import cats.implicits._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class FixedConcurrencyIOSuite extends munit.FunSuite {
  implicit val ec = ExecutionContext.global
  implicit val timer = IO.timer(ec)

  def newSubject(nConcurrency: Int): IO[FixedConcurrencyIO[IO]] = {
    implicit val cs = IO.contextShift(ec)
    implicit val ce = ConcurrentEffect[IO]
    Semaphore[IO](nConcurrency) map { semaphore =>
      new FixedConcurrencyIO[IO](semaphore)
    }
  }

  def sleep(millis: Int): IO[Unit] = {
    IO.sleep(FiniteDuration(millis, TimeUnit.MILLISECONDS))
  }

  def now: IO[Long] =
    timer.clock.realTime(scala.concurrent.duration.MILLISECONDS)

  def measure[A](fa: IO[A]): IO[(A, Long)] =
    for {
      start <- now
      a <- fa
      end <- now
    } yield (a, end - start)

  test(".all should execute given IO sequentially when concurrency = 1") {
    val (result, duration) = (for {
      subject <- newSubject(1)
      result <- measure(subject.all(List(1, 2, 3))(index => sleep(500) *> IO(index)))
    } yield result).unsafeRunSync
    assertEquals(result, List(1, 2, 3))
    assert(Math.abs(duration - 1500) < 150)
  }

  test(".all should execute given IO concurrently when concurrency > 1") {
    val (result, duration) = (for {
      subject <- newSubject(3)
      result <- measure(subject.all(List(1, 2, 3))(index => sleep(500) *> IO(index)))
    } yield result).unsafeRunSync
    assertEquals(result, List(1, 2, 3))
    assert(Math.abs(duration - 500) < 100)
  }

}
