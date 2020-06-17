package me.jooohn.dogeared.lambda

import cats.effect.{ExitCode, IO}
import me.jooohn.dogeared.lambda.LambdaRuntimeApi.{InvocationFailure, InvocationSuccess}

class LambdaRuntime(runtimeAPI: LambdaRuntimeApi, handler: Handler) {

  def run: IO[ExitCode] =
    for {
      _ <- process
      exitCode <- run
    } yield exitCode

  def process: IO[Unit] =
    runtimeAPI.nextInvocation flatMap { invocation =>
      handler(invocation).attempt.flatMap {
        case Right(success @ InvocationSuccess(_)) =>
          runtimeAPI.invocationResponse(invocation.requestId, success)
        case Right(failure @ InvocationFailure(_, _)) =>
          runtimeAPI.invocationError(invocation.requestId, failure)
        case Left(e) =>
          IO(println(e)) *> runtimeAPI.invocationError(
            invocation.requestId,
            InvocationFailure("RuntimeException", e.toString)
          )
      }
    }
}
