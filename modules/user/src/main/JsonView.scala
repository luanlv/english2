package lila.user

import lila.common.PimpedJson._
import play.api.libs.json._
import User.{ PlayTime }

final class JsonView(isOnline: String => Boolean) {

  import JsonView._
  private implicit val profileWrites = Json.writes[Profile]
  private implicit val playTimeWrites = Json.writes[PlayTime]

  def apply(u: User) = Json.obj(
    "id" -> u.id,
    "username" -> u.username,
    "title" -> u.title,
    "online" -> isOnline(u.id),
    "engine" -> u.engine,
    "booster" -> u.booster,
    "language" -> u.lang,
    "profile" -> u.profile.??(profileWrites.writes).noNull,
    "createdAt" -> u.createdAt,
    "seenAt" -> u.seenAt,
    "playTime" -> u.playTime
  ).noNull

}

object JsonView {

  implicit val nameWrites = Writes[User] { u =>
    JsString(u.username)
  }


  implicit val modWrites = OWrites[User] { u =>
    Json.obj(
      "id" -> u.id,
      "username" -> u.username,
      "title" -> u.title,
      "engine" -> u.engine,
      "booster" -> u.booster,
      "troll" -> u.troll,
      "games" -> u.count.game).noNull
  }

}
