package me.jooohn.dogeared.drivenadapters

import java.util.concurrent.TimeUnit

import cats.effect.IO
import cats.effect.concurrent.Ref
import me.jooohn.dogeared.drivenports.JobSuccess
import me.jooohn.dogeared.util.IOContext

import scala.concurrent.duration.{Duration, _}

class CatsAsyncJobSuite extends munit.FunSuite with IOContext {

  override def munitFixtures = List(testContextFixture)

  test("runs F asynchronously") {
    val testContext = testContextFixture()
    implicit val cs = testContext.ioContextShift
    val timer = testContext.ioTimer
    val io = (CatsAsyncJob
      .resource[IO](1)
      .use { asyncJob =>
        for {
          ref <- Ref.of[IO, List[String]](Nil)
          _ <- asyncJob.later(for {
            _ <- timer.sleep(Duration(1, TimeUnit.SECONDS))
            _ <- ref.update(_ :+ "job finished")
          } yield JobSuccess)
          _ <- ref.update(_ :+ "job started")
          _ <- timer.sleep(Duration(2, TimeUnit.SECONDS))
          result <- ref.get
        } yield result
      })
      .unsafeToFuture()
    testContext.tick(4.seconds)
    val result = io.value.get.get
    assertEquals(result, List("job started", "job finished"))
  }

  test("cancels async job if returned") {
    val testContext = testContextFixture()
    implicit val cs = testContext.ioContextShift
    val timer = testContext.ioTimer
    val io = (CatsAsyncJob
      .resource[IO](1)
      .use { asyncJob =>
        for {
          ref <- Ref.of[IO, List[String]](Nil)
          _ <- asyncJob.later(for {
            _ <- timer.sleep(Duration(1, TimeUnit.SECONDS))
            _ <- ref.update(_ :+ "job finished")
          } yield JobSuccess)
          _ <- ref.update(_ :+ "job started")
          // Finish before async job finished.
          result <- ref.get
        } yield result
      })
      .unsafeToFuture()
    testContext.tick(2.seconds)
    val result = io.value.get.get
    assertEquals(result, List("job started"))
  }

  test("queueing works (concurrency = 1)") {
    val testContext = testContextFixture()
    implicit val cs = testContext.ioContextShift
    val timer = testContext.ioTimer
    val io = (CatsAsyncJob
      .resource[IO](1)
      .use { asyncJob =>
        for {
          ref <- Ref.of[IO, List[String]](Nil)
          _ <- asyncJob.later(ref.update(_ :+ "job1") as JobSuccess)
          _ <- asyncJob.later(ref.update(_ :+ "job2") as JobSuccess)
          _ <- asyncJob.later(ref.update(_ :+ "job3") as JobSuccess)
          _ <- asyncJob.later(ref.update(_ :+ "job4") as JobSuccess)
          _ <- timer.sleep(1.second)
          result <- ref.get
        } yield result
      })
      .unsafeToFuture()
    testContext.tick(2.seconds)
    val result = io.value.get.get
    assertEquals(result, List("job1", "job2", "job3", "job4"))
  }

  test("queueing works (concurrency = 4)") {
    val testContext = testContextFixture()
    implicit val cs = testContext.ioContextShift
    val timer = testContext.ioTimer
    val io = (CatsAsyncJob
      .resource[IO](3)
      .use { asyncJob =>
        for {
          ref <- Ref.of[IO, List[String]](Nil)
          _ <- asyncJob.later(timer.sleep(1.second) *> ref.update(_ :+ "job1") as JobSuccess)
          _ <- asyncJob.later(timer.sleep(1.second) *> ref.update(_ :+ "job2") as JobSuccess)
          _ <- asyncJob.later(timer.sleep(1.second) *> ref.update(_ :+ "job3") as JobSuccess)
          _ <- asyncJob.later(timer.sleep(1.second) *> ref.update(_ :+ "job4") as JobSuccess)
          _ <- timer.sleep(1.5.second)
          result <- ref.get
        } yield result
      })
      .unsafeToFuture()
    testContext.tick(2.seconds)
    val result = io.value.get.get
    assertEquals(result.length, 3)
  }

}
