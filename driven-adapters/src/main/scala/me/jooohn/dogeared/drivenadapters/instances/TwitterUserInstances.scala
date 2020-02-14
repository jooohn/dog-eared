package me.jooohn.dogeared.drivenadapters.instances

import doobie.util.Write
import me.jooohn.dogeared.domain.TwitterUser

trait TwitterUserInstances {

  implicit val twitterUserWrite: Write[TwitterUser] =
    Write[(String, String)].contramap(tu => (tu.id, tu.username))

}
