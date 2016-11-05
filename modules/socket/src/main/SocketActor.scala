package lila.socket

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.Random
import akka.actor.{Deploy => _, _}
import play.api.libs.json._
import play.twirl.api.Html
import actorApi.{SetAlive, _}
import lila.common.LightUser
import lila.hub.actorApi.chatRoom._
import lila.hub.actorApi.{Deploy, GetUids, HasUserId, SocketUids}
import lila.memo.ExpireSetMemo
import akka.pattern.{ ask, pipe }
import makeTimeout.large

abstract class SocketActor[M <: SocketMember](uidTtl: Duration) extends Socket with Actor {

  val listRoomIds = List("01", "02", "03", "04", "05", "06")
  val listSid  = scala.collection.mutable.Map.empty[String, (Option[String], String)]

  val listSidPost  = scala.collection.mutable.Map.empty[String, String]

  val listSidQuestion  = scala.collection.mutable.Map.empty[String, String]

  val members = scala.collection.mutable.Map.empty[String, M]
  val aliveUids = new ExpireSetMemo(uidTtl)
  var pong = initialPong

  val lilaBus = context.system.lilaBus

  // this socket is created during application boot
  // and therefore should delay its publication
  // to ensure the listener is ready (sucks, I know)
  val startsOnApplicationBoot: Boolean = false

  override def preStart {
    if (startsOnApplicationBoot)
      context.system.scheduler.scheduleOnce(1 second) {
        lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)
      }
    else lilaBus.publish(lila.socket.SocketHub.Open(self), 'socket)
  }

  override def postStop() {
    super.postStop()
    lilaBus.publish(lila.socket.SocketHub.Close(self), 'socket)
    members.keys foreach eject
  }

  // to be defined in subclassing actor
  def receiveSpecific: Receive

  // generic message handler
  def receiveGeneric: Receive = {

    case Ping(uid, notify, makeFriend)      => ping(uid, notify, makeFriend)

    case Broom             => broom

    case NbMembers(nb)                     => {
      pong = pong + ("d" -> JsNumber(nb))
    }

    // when a member quits
    case Quit(uid)         => {
      if(listSid.keySet.contains(uid)) unSub(uid)
      if(listSidPost.keySet.contains(uid)) unSubPost(uid)
      if(listSidQuestion.keySet.contains(uid)) unSubQuestion(uid)
      quit(uid)
    }

    case GetUids           => sender ! SocketUids(members.keySet.toSet)

    case HasUserId(userId) => sender ! members.values.exists(_.userId.contains(userId))

    case Resync(uid)       => resync(uid)

    case d: Deploy         => onDeploy(d)


    case SetAlive(uid)         => setAlive(uid)

    case SendName(uid, id, name)   => sendName(uid, id,  name)

    case SendOnlineFriends(uid, listUser) => sendOnlineFriend(uid, listUser)

    case SendFriendsList(uid, listUser) => sendFriendsList(uid, listUser)

    case SendMissingMes(uid, f, t, data) => sendMissingMes(uid, f, t, data)

    case SendInitMes(uid, data) => sendInitMes(uid, data)

    case SendInitPost(uid, posts) => sendInitPost(uid, posts)

    case SendMorePost(uid, posts) => sendMorePost(uid, posts)

    case SendInitQA(uid, questions) => sendInitQA(uid, questions)

    case SendInitNotify(uid, data) => sendInitNotify(uid, data)

    case InitChatRoom(uid, roomId, userId) => {
      if(roomId != "chatrooms") initChat(uid, roomId, userId)
      else initChatRooms(uid, roomId, userId)
    }

    case GetPrevChat(uid, roomId, lastTime) => getPrevChat(uid, roomId, lastTime)

    case Sub(uid, roomId, userId) => sub(uid, roomId, userId)

    case SubPost(uid, postId) => subPost(uid, postId)

    case SubQuestion(uid, questionId) => subQuestion(uid, questionId)

    case UnSub(uid, roomId) => unSub(uid, roomId)

    case UnSubPost(uid) => unSubPost(uid)

    case UnSubQuestion(uid) => unSubQuestion(uid)

    case SendNewAnswer(questionId, answer) => sendNewAnswer(questionId, answer)

    case SendNewComment(postId, comment) => sendNewComment(postId, comment)

    case SendMoreComment(uid, postId, comment) => sendMoreComment(uid, postId, comment)

    case SendNewCommentQA(questionId, comment) => sendNewCommentQA(questionId, comment)

    case UserEnterRoom(user, roomId) => notifyUserEnterRoom(user, roomId)

    case UserLeavesRoom(user, roomId) => notifyUserLeaveRoom(user, roomId)

    case DoChat(chat, roomId) =>  sendChatRoom(chat, roomId)

    case SendFriendRequest(uid, data) => sendFriendRequester(uid, data)
  }

  def receive = receiveSpecific orElse receiveGeneric

  def notifyAll[A: Writes](t: String, data: A) {
    notifyAll(makeMessage(t, data))
  }

  def notifyAll(t: String) {
    notifyAll(makeMessage(t))
  }

  def notifyAll(msg: JsObject) {
    members.values.foreach(_ push msg)
  }

  def notifyIf[A: Writes](pred: SocketMember => Boolean, t: String, data: A) {
    val msg = makeMessage(t, data)
    members.values.filter(pred).foreach(_ push msg)
  }

  def notifyAllAsync[A: Writes](t: String, data: A) = Future {
    notifyAll(t, data)
  }

  def notifyAllAsync(t: String) = Future {
    notifyAll(t)
  }

  def notifyAllAsync(msg: JsObject) = Future {
    notifyAll(msg)
  }

  def notifyMember[A: Writes](t: String, data: A)(member: M) {
    member push makeMessage(t, data)
  }

  def notifyUid[A: Writes](t: String, data: A)(uid: Socket.Uid) {
    withMember(uid.value)(_ push makeMessage(t, data))
  }

  def ping(uid: String, notify: Int, makeFriend: Int) {
    setAlive(uid)
    val msg = makeMessage("n", pong.++(Json.obj("n" -> notify, "mf" -> makeFriend)))
    withMember(uid)(_ push msg)
  }

  def listUidInRoom(roomId: String) = {
    listSid collect {
      case (uid, (_, idRoom)) if (roomId == idRoom) =>  uid
    }
  }

  def notifyUserEnterRoom(user: JsValue, roomId: String) {
    val mes = Json.obj("room" -> roomId, "t" -> "userEnter", "u" -> user)
    listUidInRoom(roomId) foreach {uid =>
      withMember(uid)(_ push makeMessage("chatNotify", mes))
    }
  }

  def notifyUserLeaveRoom(user: String, roomId: String) = {
    val mes = Json.obj("room" -> roomId, "t" -> "userLeaves", "u" -> user)
    listUidInRoom(roomId) foreach {uid =>
      withMember(uid)(_ push makeMessage("chatNotify", mes))
    }
  }

  def sendChatRoom(chat: JsObject, roomId: String) = {
    listUidInRoom(roomId) foreach { uid =>
      withMember(uid)(_ push makeMessage("chatNotify", chat))
    }
  }



  def initChatRooms(uid: String, roomId: String, userId:Option[String]) = {
    sub(uid, roomId, userId)
    val listByGroup = listSid.groupBy(x => x._2._2)
    val data = listRoomIds.map(roomId => Json.obj("id" -> roomId, "c" -> (if(listByGroup.contains(roomId)) listByGroup(roomId).toSeq.length else 0), "u" -> (if(listByGroup.contains(roomId)) listByGroup(roomId).values.toList.map(x => x._1).distinct.size else 0)))
    listUidInRoom(roomId) foreach { uid =>
      withMember(uid)(_ push makeMessage("chatNotify", Json.obj("t" -> "initChatRooms" , "v" -> data)))
    }
  }

  def initChat(uid: String, roomId: String, userId:Option[String]) = {
    sub(uid, roomId, userId)
    (lila.hub.Env.current.actor.chatRoom ? GetInitChatRoom(roomId)) foreach {
      case data: JsObject => {
        val listByGroup = listSid.groupBy(x => x._2._2)
        val newData = data.++(Json.obj("c" -> listByGroup(roomId).toSeq.length, "u" -> listByGroup(roomId).values.toList.map(x => x._1).distinct.size))
        withMember(uid)(_ push makeMessage("chatNotify", newData))
      }
    }
  }



  def getPrevChat(uid: String, roomId: String, lastTime: Long) = {
    (lila.hub.Env.current.actor.chatRoom ? PrevChat(roomId, lastTime)) foreach {
      case data: JsValue => {
        val mes = Json.obj("room" -> roomId, "t" -> "prevChat" , "lc" -> data)
        withMember(uid)(_ push makeMessage("chatNotify", mes))
      }
    }
  }

  def changeNBMember(roomId: String, data: JsObject) = {
    listUidInRoom(roomId) foreach { uid =>
      withMember(uid)(_ push makeMessage("chatNotify", data))
    }
  }

  def sendNewComment(postId: String, comment: JsValue) = {
    listSidPost collect {
      case (uid, pId) if (pId == postId) =>  uid
    } foreach { uid =>
      withMember(uid)(_ push makeMessage("newComment", comment))
    }
  }

  def sendMoreComment(uid: String, postId: String, comment: JsValue) = {
    withMember(uid)(_ push makeMessage("moreComment", comment))
  }

  def sendNewCommentQA(questionId: String, comment: JsValue) = {
    listSidQuestion collect {
      case (uid, pId) if (pId == questionId) =>  uid
    } foreach { uid =>
      withMember(uid)(_ push makeMessage("newCommentQA", comment))
    }
  }


  def sendNewAnswer(questionId: String, answer: JsValue) = {
    listSidQuestion collect {
      case (uid, pId) if (pId == questionId) =>  uid
    } foreach { uid =>
      withMember(uid)(_ push makeMessage("newAnswer", answer))
    }
  }

  def subPost(uid: String, postId: String) = {
    if(!listSidPost.contains(uid)){
      listSidPost += (uid -> postId)
    }
  }

  def subQuestion(uid: String, questionId: String) = {
    if(!listSidQuestion.contains(uid)){
      listSidQuestion += (uid -> questionId)
    }
  }

  def unSubPost(uid: String) = {
    listSidPost -= uid
  }

  def unSubQuestion(uid: String) = {
    listSidQuestion -= uid
  }

  def sub(uid: String, roomId: String, userId: Option[String]) = {
    if(!listSid.contains(uid)){
      userId foreach { userId =>
        if(userId.length > 0){
          if(roomId != "chatrooms") {
            if(!userInRoom(userId, roomId, listSid.keys.toList)) {
              listSid += (uid -> (Some(userId), roomId))
              val data = Json.obj("t" -> "io", "v" -> Json.obj("rid" -> roomId, "c" -> 1, "u" -> 1))
              changeNBMember("chatrooms", data)
              changeNBMember(roomId, data)
            } else {
              listSid += (uid -> (Some(userId), roomId))
              val data = Json.obj("t" -> "io", "v" -> Json.obj("rid" -> roomId, "c" -> 1))
              changeNBMember("chatrooms", data)
              changeNBMember(roomId, data)
            }
          } else {
            listSid += (uid -> (Some(userId), roomId))
          }
        } else {
          listSid += (uid -> (Some(userId), roomId))
          if(roomId != "chatrooms") {
            val data = Json.obj("t" -> "io", "v" -> Json.obj("rid" -> roomId, "c" -> 1))
            changeNBMember("chatrooms", data)
            changeNBMember(roomId, data)
          }
        }
      }
    }
  }


  def unSub(uid: String):Unit = {
    unSub(uid, listSid(uid)._2)
  }

  def unSub(uid: String, roomId: String):Unit = {
    listSid(uid)._1 foreach { userId =>
      if(userId.length > 0){
        listSid -= uid
        if(!userInRoom(userId, roomId, listSid.keys.toList)) {
          lila.hub.Env.current.actor.chatRoom ! UserUnSubscribe(userId, roomId)
          if(roomId != "chatrooms" ){
            val data = Json.obj("t" -> "io", "v" -> Json.obj("rid" -> roomId, "u" -> -1, "c" -> -1))
            changeNBMember(roomId, data)
            changeNBMember("chatrooms", data)
          }
        } else {
          listSid -= uid
          val data = Json.obj("t" -> "io", "v" -> Json.obj("rid" -> roomId, "c" -> -1))
          changeNBMember(roomId, data)
          changeNBMember("chatrooms", data)
        }
      } else {
        listSid -= uid
        val data = Json.obj("t" -> "io", "v" -> Json.obj("rid" -> roomId, "c" -> -1))
        changeNBMember(roomId, data)
        changeNBMember("chatrooms", data)
      }

    }
  }

  def userInRoom(userId: String, roomId: String, sids: List[String]):Boolean = {
    if (sids.isEmpty)  false
    else if(listSid(sids.head)._1.contains(userId) && listSid(sids.head)._2 == roomId) true
    else userInRoom(userId, roomId, sids.tail)
  }


  def uidByUserId(userId: String): Iterable[String] = members collect {
    case (uid , member) if member.userId.contains(userId) => uid
  }

  def broom {
    members.keys foreach { uid =>
      if (!aliveUids.get(uid)) eject(uid)
    }

    listSid.keys foreach { uid =>
      if (!aliveUids.get(uid)) {
        unSub(uid)
      }
    }

    listSidPost.keys foreach { uid =>
      if (!aliveUids.get(uid)) {
        listSidPost -= uid
      }
    }

    listSidQuestion.keys foreach { uid =>
      if (!aliveUids.get(uid)) {
        listSidQuestion -= uid
      }
    }

  }

  def eject(uid: String) {
    withMember(uid) { member =>
      member.end
      quit(uid)
    }
  }

  def quit(uid: String) {
    members get uid foreach { member =>
      members -= uid
      lilaBus.publish(SocketLeave(uid, member), 'socketDoor)
    }
  }


  def onDeploy(d: Deploy) {
    notifyAll(makeMessage(d.key))
  }

  private val resyncMessage = makeMessage("resync")

  protected def resync(member: M) {
    import scala.concurrent.duration._
    context.system.scheduler.scheduleOnce((Random nextInt 2000).milliseconds) {
      resyncNow(member)
    }
  }

  protected def resync(uid: String) {
    withMember(uid)(resync)
  }

  protected def resyncNow(member: M) {
    member push resyncMessage
  }

  def addMember(uid: String, member: M) {
    eject(uid)
    members += (uid -> member)
    setAlive(uid)
    lilaBus.publish(SocketEnter(uid, member), 'socketDoor)
  }

  def setAlive(uid: String) { aliveUids put uid }

  def sendName(uid: String, id: String, name: String) {
    withMember(uid)(_ push makeMessage("nu", Json.obj("id" -> id, "n" -> name)))
  }

  def sendOnlineFriend(uid: String, listUser: List[LightUser]) {
    withMember(uid)(_ push makeMessage("following_onlines", listUser))
  }

  def sendFriendsList(uid: String, listUser: List[LightUser]) {
    withMember(uid)(_ push makeMessage("friends_list", listUser))
  }

  def sendInitPost(uid: String, posts: JsValue) = {
    withMember(uid)(_ push makeMessage("initPost", posts))
  }

  def sendMorePost(uid: String, posts: JsValue) = {
    withMember(uid)(_ push makeMessage("morePost", posts))
  }

  def sendInitQA(uid: String, questions: JsValue) = {
    withMember(uid)(_ push makeMessage("initQA", questions))
  }

  def sendMissingMes(uid: String, f: Int, t: Int, data: List[JsValue]){
    withMember(uid)(_ push makeMessage("smm", Json.obj("f" -> f, "t" -> t, "d" -> data)))
  }

  def sendInitMes(uid: String, data: List[JsValue]) {
    withMember(uid)(_ push makeMessage("init_chat", data))
  }

  def sendInitNotify(uid: String, data: List[JsValue]) = {
    withMember(uid)(_ push makeMessage("init_notify", data))
  }

  def sendFriendRequester(uid: String, data: Set[LightUser]) = {
    withMember(uid)(_ push makeMessage("init_friend_request", data))
  }

  def uids = members.keys


  def membersByUserId(userId: String): Iterable[M] = members collect {
    case (_, member) if member.userId.contains(userId) => member
  }

  def uidToUserId(uid: Socket.Uid): Option[String] = members get uid.value flatMap (_.userId)

  def userIds: Iterable[String] = members.values.flatMap(_.userId)

  val maxSpectatorUsers = 10

  def showSpectators(lightUser: String => Option[LightUser])(watchers: Iterable[SocketMember]): JsValue = {

    val (total, anons, userIds) = watchers.foldLeft((0, 0, Set.empty[String])) {
      case ((total, anons, userIds), member) => member.userId match {
        case Some(userId) if !userIds(userId) && userIds.size < maxSpectatorUsers => (total + 1, anons, userIds + userId)
        case Some(_) => (total + 1, anons, userIds)
        case _ => (total + 1, anons + 1, userIds)
      }
    }

    if (total == 0) JsNull
    else if (userIds.size >= maxSpectatorUsers) Json.obj("nb" -> total)
    else Json.obj(
      "nb" -> total,
      "users" -> userIds.flatMap { lightUser(_) }.map(_.titleName),
      "anons" -> anons)
  }

  def withMember(uid: String)(f: M => Unit) {
    members get uid foreach f
  }
}
