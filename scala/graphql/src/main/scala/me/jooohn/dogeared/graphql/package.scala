package me.jooohn.dogeared

import zio.Has

package object graphql {
  type EnvWith[R] = R with GraphQLContextRepository

  type GraphQLContextRepository = Has[GraphQLContextRepository.Service]
}
