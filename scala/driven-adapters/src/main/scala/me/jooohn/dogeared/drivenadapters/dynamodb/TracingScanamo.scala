package me.jooohn.dogeared.drivenadapters.dynamodb

import cats.Monad
import me.jooohn.dogeared.drivenports.Context
import org.scanamo.ScanamoCats
import org.scanamo.ops.ScanamoOps

case class TracingScanamo[F[_]](scanamo: ScanamoCats[F])(implicit F: Monad[F]) {

  def exec[A](ops: ScanamoOps[A])(implicit ctx: Context[F]): F[A] =
    ctx.tracer.span("scanamo")(scanamo.exec(ops))

}
