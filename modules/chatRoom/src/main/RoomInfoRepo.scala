package lila.chatRoom

import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._

import spray.caching.{ LruCache, Cache }

import lila.common.LightUser
//import BSONHandlers._
import lila.db.BSON._
import scala.concurrent.ExecutionContext.Implicits.global


object RoomInfoRepo {

  private lazy val coll = Env.current.roomInfoColl


}
