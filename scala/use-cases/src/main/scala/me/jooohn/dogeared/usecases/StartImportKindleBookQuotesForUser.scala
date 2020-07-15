package me.jooohn.dogeared.usecases

import cats.effect.Async
import cats.syntax.all._
import me.jooohn.dogeared.domain.TwitterUserId
import me.jooohn.dogeared.drivenports.{AsyncJob, Context, Job, JobError, JobSuccess}

case class StartImportKindleBookQuotesForUser[F[_]: Async](
    importKindleBookQuotesForUser: ImportKindleBookQuotesForUser[F],
    asyncJob: AsyncJob[F]
) {
  import EnsureTwitterUserExistence._

  def apply(twitterUserId: TwitterUserId, importOption: ImportKindleBookQuotesForUser.ImportOption)(
      implicit ctx: Context): F[Job.Id] =
    asyncJob.later(importKindleBookQuotesForUser(twitterUserId, importOption) map {
      case Left(UserNotFound(id)) => JobError(s"twitter user ${id} not found")
      case Right(_)               => JobSuccess
    })(ctx.withAttributes("twitterUserId" -> twitterUserId, "importOption" -> importOption))

}
