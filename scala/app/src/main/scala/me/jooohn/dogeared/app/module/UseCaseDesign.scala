package me.jooohn.dogeared.app.module

import me.jooohn.dogeared.usecases.{ImportKindleBookQuotesForAllUsers, ImportKindleBookQuotesForUser}

trait UseCaseDesign { self: DSLBase with AdapterDesign =>

  def importKindleBookQuotesForUser: Bind[ImportKindleBookQuotesForUser[Effect]] = derive.singleton
  def importKindleBookQuotesForAllUsers: Bind[ImportKindleBookQuotesForAllUsers[Effect]] = derive.singleton

}
