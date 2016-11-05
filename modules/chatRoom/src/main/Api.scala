package lila.chatRoom

import akka.actor.ActorSelection
import lila.common.LightUser
import org.joda.time.DateTime
import scala.util.Success
import org.joda.time.DateTime

import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.bson._

import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
//import lila.usrMessage.MessageRepo

final class Api(
                   cached: Cached,
                   actor: ActorSelection,
                   bus: lila.common.Bus) {


  def insertChat(roomId: String, user: LightUser, chat: String, time: DateTime) = {
    RoomMessageRepo.insert(roomId, user, chat, time)
  }

  def initChatByRoom(roomId: String) = {
    RoomMessageRepo.initChatByRoom(roomId)
  }

  def getPrevChatByRoomWithTime(roomId: String, lastTime: Long) = {
    RoomMessageRepo.getPrevChatByRoomWithTime(roomId, lastTime)
  }

}
