package me.jooohn.dogeared.util

import cats.effect.laws.util.TestContext

trait IOContext extends { self: munit.FunSuite =>

  val ioContext = new Fixture[TestContext]("testContext") {
    override def apply(): TestContext = TestContext()
  }

}
