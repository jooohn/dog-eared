package me.jooohn.dogeared.app.module

import cats.effect.IO
import me.jooohn.dogeared.drivenports._
import me.jooohn.dogeared.usecases.{ImportKindleBookQuotesForAllUsers, ImportKindleBookQuotesForUser}

trait UseCaseDesign { self: DSLBase with AdapterDesign =>

  def importKindleBookQuotesForUser: Bind[ImportKindleBookQuotesForUser[IO]] = derive.singleton
  def importKindleBookQuotesForAllUsers: Bind[ImportKindleBookQuotesForAllUsers[IO]] = derive.singleton

}
