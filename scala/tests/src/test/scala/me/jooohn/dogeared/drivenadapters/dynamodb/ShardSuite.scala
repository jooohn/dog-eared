package me.jooohn.dogeared.drivenadapters.dynamodb

import munit.ScalaCheckSuite
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop._

class ShardSuite extends ScalaCheckSuite {

  property("determineShard returns 0 <= n < size") {
    val gen = for {
      string <- Arbitrary.arbitrary[String]
      posNum <- Gen.posNum[Int]
    } yield (string, posNum)
    forAll(gen) {
      case (string, shardSize) =>
        val shard = Shard.determine(string, Shard.size(shardSize))
        0 <= shard && shard < shardSize
    }
  }
}
