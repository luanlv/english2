package lila.hub
package actorApi

import lila.common.LightUser

import play.api.libs.json._
import play.twirl.api.Html

case class SendTo(userId: String, message: JsObject)

object SendTo {
  def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
    SendTo(userId, Json.obj("t" -> typ, "d" -> data))
}

case class SendTos(userIds: Set[String], message: JsObject)

object SendTos {
  def apply[A: Writes](userIds: Set[String], typ: String, data: A): SendTos =
    SendTos(userIds, Json.obj("t" -> typ, "d" -> data))
}

sealed abstract class Deploy(val key: String)
case object DeployPre extends Deploy("deployPre")
case object DeployPost extends Deploy("deployPost")
case class StreamsOnAir(html: String)

package map {
case class Get(id: String)
case class Tell(id: String, msg: Any)
case class TellIds(ids: Seq[String], msg: Any)
case class TellAll(msg: Any)
case class Ask(id: String, msg: Any)
case class Exists(id: String)
}

case class WithUserIds(f: Iterable[String] => Unit)

case object GetUids
case object GetUserIds
case class SocketUids(uids: Set[String])
case class HasUserId(userId: String)

package report {
case class Cheater(userId: String, text: String)
case class Clean(userId: String)
case class Check(userId: String)
case class MarkCheater(userId: String, by: String)
case class MarkTroll(userId: String, by: String)
case class Shutup(userId: String, text: String)
case class Booster(userId: String, accomplice: String)
}

package shutup {
case class RecordPublicForumMessage(userId: String, text: String)
case class RecordTeamForumMessage(userId: String, text: String)
case class RecordPrivateMessage(userId: String, toUserId: String, text: String)
case class RecordPrivateChat(chatId: String, userId: String, text: String)
case class RecordPublicChat(chatId: String, userId: String, text: String)
}

package mod {
case class MarkCheater(userId: String)
case class MarkBooster(userId: String)
case class ChatTimeout(mod: String, user: String, reason: String)
}

package captcha {
case object AnyCaptcha
case class GetCaptcha(id: String)
case class ValidCaptcha(id: String, solution: String)
}

package lobby {
case class ReloadTournaments(html: String)
case class ReloadSimuls(html: String)
case object NewForumPost
}

package simul {
case object GetHostIds
case class PlayerMove(gameId: String)
}

package slack {
sealed trait Event
case class Error(msg: String) extends Event
case class Warning(msg: String) extends Event
case class Info(msg: String) extends Event
case class Victory(msg: String) extends Event
}

package timeline {
case class ReloadTimeline(user: String)

sealed abstract class Atom(val channel: String, val okForKid: Boolean)
case class Follow(u1: String, u2: String) extends Atom("follow", true)
case class TeamJoin(userId: String, teamId: String) extends Atom("teamJoin", false)
case class TeamCreate(userId: String, teamId: String) extends Atom("teamCreate", false)
case class ForumPost(userId: String, topicId: Option[String], topicName: String, postId: String) extends Atom(s"forum:${~topicId}", false)
case class NoteCreate(from: String, to: String) extends Atom("note", false)
case class TourJoin(userId: String, tourId: String, tourName: String) extends Atom("tournament", true)
case class QaQuestion(userId: String, id: Int, title: String) extends Atom("qa", true)
case class QaAnswer(userId: String, id: Int, title: String, answerId: Int) extends Atom("qa", true)
case class QaComment(userId: String, id: Int, title: String, commentId: String) extends Atom("qa", true)
case class GameEnd(playerId: String, opponent: Option[String], win: Option[Boolean], perf: String) extends Atom("gameEnd", true)
case class SimulCreate(userId: String, simulId: String, simulName: String) extends Atom("simulCreate", true)
case class SimulJoin(userId: String, simulId: String, simulName: String) extends Atom("simulJoin", true)
case class StudyCreate(userId: String, studyId: String, studyName: String) extends Atom("studyCreate", true)
case class StudyLike(userId: String, studyId: String, studyName: String) extends Atom("studyLike", true)
case class PlanStart(userId: String) extends Atom("planStart", true)

object propagation {
  sealed trait Propagation
  case class Users(users: List[String]) extends Propagation
  case class Followers(user: String) extends Propagation
  case class Friends(user: String) extends Propagation
  case class StaffFriends(user: String) extends Propagation
  case class ExceptUser(user: String) extends Propagation
}

import propagation._

case class Propagate(data: Atom, propagations: List[Propagation] = Nil) {
  def toUsers(ids: List[String]) = add(Users(ids))
  def toUser(id: String) = add(Users(List(id)))
  def toFollowersOf(id: String) = add(Followers(id))
  def toFriendsOf(id: String) = add(Friends(id))
  def toStaffFriendsOf(id: String) = add(StaffFriends(id))
  def exceptUser(id: String) = add(ExceptUser(id))
  private def add(p: Propagation) = copy(propagations = p :: propagations)
}
}

package game {
case class ChangeFeatured(id: String, msg: JsObject)
case object Count
}

package tv {
case class Select(msg: JsObject)
}

package notify {
case class Notified(userId: String)
}

package forum {
case class MakeTeam(id: String, name: String)
}

package fishnet {
case class AutoAnalyse(gameId: String)
}

package round {
case class MoveEvent(
  gameId: String,
  fen: String,
  move: String,
  mobilePushable: Boolean,
  opponentUserId: Option[String],
  simulId: Option[String])
case class NbRounds(nb: Int)
case class Abort(gameId: String, byColor: String)
case class Berserk(gameId: String, userId: String)
sealed trait SocketEvent
object SocketEvent {
  case class OwnerJoin(gameId: String , ip: String) extends SocketEvent
  case class Stop(gameId: String) extends SocketEvent
}
case class FishnetPlay()
}

package evaluation {
case class AutoCheck(userId: String)
case class Refresh(userId: String)
}

package bookmark {
case class Toggle(gameId: String, userId: String)
case class Remove(gameId: String)
}

package relation {
case class ReloadOnlineFriends(userId: String)
case class GetOnlineFriends(userId: String)
case class OnlineFriends(users: List[LightUser], playing: Set[String]) {
  def patrons: List[String] = users collect {
    case u if true => u.id
  }
}
object OnlineFriends {
  val empty = OnlineFriends(Nil, Set.empty[String])
}
case class Block(u1: String, u2: String)
case class UnBlock(u1: String, u2: String)

case class GetFriendRequest(userId: String)
case class GetOnlineUser(userId: String)
case class GetFriends(userId: String)

}

package plan {
case class ChargeEvent(username: String, amount: Int, percent: Int)
}

package userMessage {
  case class Msg(userId: String, o: JsObject)
  case class PingVersion(userId: String)
  case class NotifyMovementOnlineUser()

  case class GetName(uid: String)
  case class InitChat(fromId: String, toId: String, cv: Int)
  case class MissingMes(userId: String, f: Int, t: Int)
  case class InitNotify(userId: String)
  case class MarkRead(userId: String, toId: String, mv: Int)
}

package chatRoom {
  case class UserSubscribe(userId: String, roomId: String)
  case class UserUnSubscribe(userId: String, roomId: String)
  case class UserEnterRoom(user: JsValue, roomId: String)
  case class UserLeavesRoom(user: String, roomId: String)
  case class GetInitChatRoom(roomId: String)
  case class ChatRoomMessage(userId: String, roomId: String, mes: String)
  case class DoChat(chat: JsObject, roomId: String)
  case class PrevChat(roomId: String, lastTime: Long)
}

package activity {
  case class InitPost(userId: String)
  case class MorePost(userId: String, time: Long)
  case class CommentPost(userId: String, postId: String, comment: String)
  case class ChildCommentPost(userId: String, postId: String, parentId: String, comment: String)
  case class MoreCommentPost(postId: String, time: Long)

}

package question {
  case class InitQA(userId: Option[String])
  case class CommentQA(userId: String, parentId: String, parentType: String, comment: String)
  case class AnswerQA(userId: String, questionId: String, answer: String)
}