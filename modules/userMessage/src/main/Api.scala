package lila.userMessage

import akka.actor.ActorSelection
import lila.common.LightUser
import org.joda.time.DateTime
import scala.util.Success


import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
//import lila.usrMessage.MessageRepo

final class Api(
                           cached: Cached,
                           actor: ActorSelection,
                           bus: lila.common.Bus) {

  def findLastMesVersion(chatId: String) = {
    cached.chatVersion(chatId)
  }

  def findLastesUserMesVersion(userId: String) = {
    cached.userChatVersion(userId)
  }

  def insert(mv: Int, fromId: LightUser, toId: LightUser, mes: String, time: DateTime) = {
    MessageRepo.insert(mv, fromId, toId, mes, time)
  }

  def getInitMes(mesId: String, cv: Int) = {
    MessageRepo.getInitMes(mesId, cv)
  }

  def getMissingMes(listMesIds: Array[String]) = {
    MessageRepo.getMissingMes(listMesIds)
  }

  def notifyMessage(uid: String, chatId: String, mesId: String, mv: Int, mes: String, time: DateTime) = {
    NotifyRepo.notifyMessage(uid, chatId, mesId, mv, mes, time)
  }

  def getNotifyMessage(userId: String) = {
    NotifyRepo.getNotifyMessage(userId)
  }

  def resetNotify(userId: String) = {
    NotifyRepo.resetNotify(userId)
  }

  def markRead(userId: String, toId: String, mv: Int) = {
    NotifyRepo.markRead(userId, toId, mv)
  }
}
