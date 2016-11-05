package lila.userMessage

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
import lila.hub.actorApi.userMessage._
import org.joda.time.DateTime
import play.api.libs.json._

import actorApi._


import lila.common.LightUser
import lila.hub.actorApi.relation._
import lila.hub.actorApi.{SendTos, SendTo}
import makeTimeout.short
import reactivemongo.bson.BSONDocument

private[userMessage] final class UserMessageActor(
                                               getOnlineUserIds: () => Set[String],
                                               lightUser: String => Option[LightUser],
                                               api: Api) extends Actor {

  private val bus = context.system.lilaBus

  private var onlines = Map[ID, LightUser]()

  def receive = {

    case PingVersion(userId) => {
      sender ! Env.current.cached.getNotify(userId).await
    }

    case Msg(userId, o) => sendMessage(userId, o)

    case GetOnlineUser(userId) => sender ! onlines.values.toList

    case InitChat(fromId, toId, cv) => {
      val mesId = if(fromId < toId) fromId + toId else toId + fromId
      sender ! api.getInitMes(mesId, cv).map(_.map(Json.toJson(_)))
    }

    case MarkRead(userId, toId, mv) => markRead(userId, toId, mv)

    case MissingMes(userId, f, t) => {
      sendMissingMes(userId, f, t)
    }

    case GetName(id) => lightUser(id) match {
      case None => sender ! "anonymous"
      case Some(user) => sender ! user.name
    }

    case InitNotify(userId) => {
      val data = api.getNotifyMessage(userId).map(_.map(Json.toJson(_)))
      sender ! data
      Env.current.cached.setNewVersion("notify:" + userId, 0)
      api.resetNotify(userId)
    }

    case NotifyMovement =>
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList
      val leaves = leaveIds.flatMap(i => lightUser(i))
      val enters = enterIds.flatMap(i => lightUser(i))
      onlines = onlines -- leaveIds ++ enters.map(e => e.id -> e)
      notifyFollowers(enters, "following_enters")
      notifyFollowers(leaves, "following_leaves")

    case _ =>
  }
  private def onlineIds: Set[ID] = onlines.keySet


  def sendMissingMes(userId: String, f: Int, t: Int) {
    val listMesIds = (f to t).toArray.map{ num =>
      Env.current.cached.mapVersion(userId, num) match {
        case None => ""
        case Some(map) => map.await.getOrElse(num, "")
      }
    }
    sender ! api.getMissingMes(listMesIds).map(_.map(Json.toJson(_)))
  }

  def markRead(userId: String, toId: String, mv: Int) = {
   api.markRead(userId, toId, mv).map{
     b => if(b) {
       val curNotify = Env.current.cached.getNotify(userId).await
       val newNotify =  if(curNotify > 0) {curNotify - 1} else curNotify
       Env.current.cached.setNewVersion("notify:" + userId, newNotify)
       bus.publish(SendTo(toId, "n", newNotify), 'users)
     }
   }

  }

  def sendMessage(fromId: String, o: JsObject) {
    val curUsers = getOnlineUserIds().flatMap(i => lightUser(i)).toList
    onlines = curUsers.map(e => e.id -> e).toMap
    (o\"to").asOpt[String] foreach { toId =>
      (o\"mes").asOpt[String] foreach { mes =>
        val mesId = if(fromId < toId) fromId + toId else toId + fromId
        var mv = api.findLastMesVersion(mesId).await
        var fromV = api.findLastesUserMesVersion(fromId).await
        var toV = api.findLastesUserMesVersion(toId).await
        var time = DateTime.now()

        api.insert(mv + 1, lightUser(fromId).head, lightUser(toId).head, mes, time) map {
          writeResult => writeResult match {
            case ok if ok.ok   => {
              Env.current.cached.setNewVersion("chatVer:" + mesId, mv + 1)
              Env.current.cached.setNewVersion("userChatVer:",fromId, fromV + 1, toId, toV + 1)
              Env.current.cached.pushVersion(toId, toV + 1, mesId + "_" + (mv + 1) )
              Env.current.cached.pushVersion(fromId, toV + 1, mesId + "_" + (mv + 1) )

              val data = Json.obj("mv" -> (mv + 1), "f" -> lightUser(fromId).head, "t" -> lightUser(toId).head, "m" -> mes, "timem" -> time)
              bus.publish(SendTo(fromId, "mes", data.++(Json.obj("v" -> (fromV + 1)))), 'users)
              bus.publish(SendTo(toId, "mes", data.++(Json.obj("v" -> (toV + 1 )))), 'users)
              if(api.notifyMessage(toId, fromId,  mesId,  mv + 1, mes, time)){
                val newNotify = Env.current.cached.getNotify(toId).await + 1
                Env.current.cached.setNewVersion("notify:" + toId, newNotify)
                bus.publish(SendTo(toId, "n", newNotify), 'users)
              } else {
//                println("not update!")
              }

            }
            case error         => //println("save mes ERROR!")
          }
        }
      }
    }
  }

  private def notifyFollowers(users: List[LightUser], message: String) {
    users foreach { user =>
        bus.publish(SendTos(onlineIds.toSet, message, user), 'users)
    }
  }


}
