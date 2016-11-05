package lila.socket
package actorApi

import play.api.libs.json.{JsObject, JsValue}
import akka.actor.ActorRef
import lila.common.LightUser

case class Connected[M <: SocketMember](
  enumerator: JsEnumerator,
  member: M)
case class Sync(uid: String, friends: List[String])
case class Ping(uid: String, n1: Int, n2: Int)
//case class PingVersion(uid: String, version: Int)
case class SetAlive(uid: String)
case class Test(uid: String)
case class Test2(uid: String, to: String, mes: String)
case object Broom
case class Quit(uid: String)

case class SocketEnter[M <: SocketMember](uid: String, member: M)
case class SocketLeave[M <: SocketMember](uid: String, member: M)

case class Resync(uid: String)

case object GetVersion

case class SendToFlag(flag: String, message: JsObject)

case object PopulationTell
case class NbMembers(nb: Int)
case class StartWatching(uid: String, member: SocketMember, gameIds: Set[String])

case class SendName(uid: String, id: String, name: String)

case class SendOnlineFriends(uid: String, list: List[LightUser])
case class SendFriendsList(uid: String, list: List[LightUser])
case class SendInitPost(uid: String, posts: JsValue)

case class SendInitQA(uid: String, questions: JsValue)

case class SendMissingMes(uid: String, f: Int, t: Int, data: List[JsValue])
case class SendInitMes(uid: String, data: List[JsValue])
case class SendFriendRequest(uid: String, data: Set[LightUser])

case class SendInitNotify(uid: String, data: List[JsValue])

case class Sub(uid: String, s: String, userId: Option[String])
case class SubPost(uid: String, postId: String)
case class SubQuestion(uid: String, questionId: String)
case class UnSub(uid: String, s: String)
case class UnSubQuestion(uid: String)
case class UnSubPost(uid: String)
case class InitChatRoom(uid: String, roomId: String, userId: Option[String])
case class InitChatRooms(uid: String)
case class GetPrevChat(uid: String, roomId: String, lastTime: Long)

case class SendNewComment(postId: String, comment: JsValue)
case class SendNewCommentQA(questionId: String, comment: JsValue)
case class SendNewAnswer(questionId: String, answer: JsValue)

case class SendMoreComment(uid: String, postId: String, c: JsValue)
case class SendMorePost(uid: String, posts: JsValue)