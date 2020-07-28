package me.jooohn.dogeared

import zio.{Has, RIO}

package object graphql {
  type Effect[A] = RIO[zio.ZEnv, A]
  type Env = zio.ZEnv with GraphQLContextRepository
  type EffectWithEnv[A] = RIO[Env, A]

  type GraphQLContextRepository = Has[GraphQLContextRepository.Service]
}
