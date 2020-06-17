package me.jooohn.dogeared

import cats.effect.IO
import lambda.LambdaRuntimeApi.{InvocationRequest, InvocationResponse}
import me.jooohn.dogeared.app.ProductionAppModule

package object lambda {

  type Handler = InvocationRequest => IO[InvocationResponse]
  type HandlerFactory = ProductionAppModule => IO[Handler]

}
