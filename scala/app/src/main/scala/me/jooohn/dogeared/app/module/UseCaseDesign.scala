package me.jooohn.dogeared.app.module

import me.jooohn.dogeared.drivenadapters.ConcurrentAsyncJob
import me.jooohn.dogeared.usecases.{
  ImportKindleBookQuotesForAllUsers,
  ImportKindleBookQuotesForUser,
  ImportUser,
  StartImportKindleBookQuotesForUser
}

trait UseCaseDesign { self: DSLBase with AdapterDesign =>

  implicit def importKindleBookQuotesForUser: Bind[ImportKindleBookQuotesForUser[Effect]] = derive.singleton
  implicit def importKindleBookQuotesForAllUsers: Bind[ImportKindleBookQuotesForAllUsers[Effect]] = derive.singleton
  implicit def importUser: Bind[ImportUser[Effect]] = derive.singleton
  implicit def startImportKindleBookQuotesForUser: Bind[StartImportKindleBookQuotesForUser[Effect]] =
    singleton(for {
      importKindleBookQuotesForUser <- inject[ImportKindleBookQuotesForUser[Effect]]
      asyncJob <- injectF(ConcurrentAsyncJob.fixedConcurrency(2))
    } yield StartImportKindleBookQuotesForUser(importKindleBookQuotesForUser, asyncJob))

}
