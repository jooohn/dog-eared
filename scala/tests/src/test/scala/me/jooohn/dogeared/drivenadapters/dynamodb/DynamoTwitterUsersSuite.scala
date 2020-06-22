package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.effect.IO
import me.jooohn.dogeared.domain.TwitterUser

import scala.util.Random

class DynamoTwitterUsersSuite extends munit.FunSuite with DynamoDBFixtures {
  import _root_.me.jooohn.dogeared.cs

  override def munitFixtures = List(scanamoFixture)

  def newTestUser(): TwitterUser = TwitterUser(
    id = Random.nextString(8),
    username = Random.nextString(8),
  )

  def newAdapter(): DynamoTwitterUsers[IO] = DynamoTwitterUsers(scanamoFixture(), ioLogger, Shard.size(4))

  test("should resolve object after stored") {
    val testUser = newTestUser()
    val adapter = newAdapter()
    val storeThenResolve = for {
      _ <- adapter.store(testUser)
      resolved <- adapter.resolve(testUser.id)
    } yield resolved
    assertEquals(storeThenResolve.unsafeRunSync().get, testUser)
  }

  test("should resolveAll after storeMany") {
    val testUser1 = newTestUser()
    val testUser2 = newTestUser()
    val adapter = newAdapter()
    val storeThenResolve = for {
      before <- adapter.resolveAll
      _ <- adapter.storeMany(List(testUser1, testUser2))
      after <- adapter.resolveAll
    } yield (before, after)
    val (before, after) = storeThenResolve.unsafeRunSync()
    val beforeSet = before.toSet
    val afterSet = after.toSet
    assertEquals(beforeSet.subsetOf(afterSet), true)
    val newSet = afterSet -- beforeSet
    assertEquals(newSet.toList.sortBy(_.id), List(testUser1, testUser2).sortBy(_.id))
  }

}
