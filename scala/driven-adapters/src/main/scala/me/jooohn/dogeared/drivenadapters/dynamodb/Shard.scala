package me.jooohn.dogeared.drivenadapters.dynamodb

object Shard {

  def determine(string: String, size: Int): Int = ((string.hashCode % size) + size) % size

}
