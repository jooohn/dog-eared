package me.jooohn.dogeared.graphql

import caliban.Value.StringValue
import caliban.schema._
import me.jooohn.dogeared.domain.{KindleBookId, TwitterUserId, TwitterUsername}

case class Id(value: String) {
  def asTwitterUserId: TwitterUserId = value
  def asTwitterUsername: TwitterUsername = value
  def asKindleBookId: KindleBookId = value
}
object Id {

  implicit val idSchema: Schema[Any, Id] =
    Schema.scalarSchema("ID", None, id => StringValue(id.value))

}
