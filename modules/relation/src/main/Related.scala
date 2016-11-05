package lila.relation

import play.api.libs.json.{Json, Writes}

case class Related(
    user: lila.user.User,
    nbGames: Option[Int],
    followable: Boolean,
    relation: Option[Relation])
{

  def toJson(implicit userWrites: Writes[lila.user.User]) = Json.obj(
    "user" -> user,
    "nbGames" -> nbGames,
    "followable" -> followable,
    "relation" -> relation)
}