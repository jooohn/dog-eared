package me.jooohn.dogeared.graphql

import caliban.Value.StringValue
import caliban.schema._

case class Id(value: String)
object Id {

  implicit val idSchema: Schema[Any, Id] =
    Schema.scalarSchema("ID", None, id => StringValue(id.value))

}
