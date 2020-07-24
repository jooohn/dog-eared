package me.jooohn.dogeared

import zio.{Has, RIO}

package object graphql {
  type Effect[A] = RIO[zio.ZEnv, A]
  type Env = zio.ZEnv with GraphQLContextRepository

  type GraphQLContextRepository = Has[GraphQLContextRepository.Service]
}
