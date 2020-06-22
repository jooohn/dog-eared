package me.jooohn.dogeared.drivenadapters.dynamodb
import shapeless.tag
import shapeless.tag._

object Shard {

  type Size = Int @@ "ShardSize"
  def size(value: Int): Size = tag["ShardSize"][Int](value)

  def determine(string: String, size: Size): Size = Shard.size(((string.hashCode % size) + size) % size)

}
