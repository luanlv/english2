package lila.user

import lila.common.LightUser

import lila.db.dsl._
import reactivemongo.bson._
import scala.concurrent.duration._
import User.{ BSONFields => F }

final class LightUserApi(coll: Coll) {

  def get(id: String): Option[LightUser] = {
    cache get id
  }

  def refresh(id: String) = cache invalidate id

  def invalidate = cache invalidate _

  private implicit val lightUserReader = new BSONDocumentReader[LightUser] {

    def read(doc: BSONDocument) =
      LightUser(
        id = doc.getAs[String](F.id) err "LightUser id missing",
        name = doc.getAs[String](F.name) err "LightUser username missing",
        title = doc.getAs[String](F.title),
        avatar = doc.getAs[String](F.avatar) err ""
      )
  }

  private val cache = lila.memo.MixedCache[String, Option[LightUser]](
    id => coll.find(
      $id(id),
      $doc(F.id -> true, F.name -> true, F.title -> true, F.avatar -> true)
    ).uno[LightUser],
    timeToLive = 20 minutes,
    default = id => LightUser(id, id, Some(""), "").some,
    logger = logger branch "LightUserApi")
}
