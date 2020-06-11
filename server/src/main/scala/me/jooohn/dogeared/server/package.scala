package me.jooohn.dogeared

import cats.effect.IO
import me.jooohn.dogeared.server.LambdaRuntimeAPI.{InvocationRequest, InvocationResponse}

package object server {

  type Handler = InvocationRequest => IO[InvocationResponse]

}
