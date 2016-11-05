package lila.chatRoom

import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._

import spray.caching.{ LruCache, Cache }

import lila.common.LightUser
import BSONHandlers._
import lila.db.BSON._
import lila.db.dsl._
import scala.concurrent.ExecutionContext.Implicits.global


object RoomMessageRepo {

  private lazy val coll = Env.current.roomMessageColl

  def insert(roomId: String, user: LightUser, chat: String, time: DateTime) = {
    coll.insert(RoomMessage(roomId, user, chat, time))
  }

  def initChatByRoom(roomId: String) = {
    val bs = BSONDocument("roomId" -> roomId)
    coll.find(bs)
      .sort(BSONDocument("time" -> -1))
      .cursor[RoomMessage]()
      .gather[List](20)
  }

  def getPrevChatByRoomWithTime(roomId: String, time: Long) = {
    val datetime = new DateTime(time)
    val bs = BSONDocument("roomId" -> roomId, "time" -> BSONDocument("$lt" -> BSONDateTime(time)))
    coll.find(bs)
        .sort(BSONDocument("time" -> -1))
        .cursor[RoomMessage]()
          .gather[List](10)
  }

}
