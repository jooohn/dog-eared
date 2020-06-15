package me.jooohn.dogeared

import cats.effect.IO
import lambda.LambdaRuntimeAPI.{InvocationRequest, InvocationResponse}

package object lambda {

  type Handler = InvocationRequest => IO[InvocationResponse]

}
