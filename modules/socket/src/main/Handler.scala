package lila.socket

import akka.actor.ActorRef
import akka.pattern.{ask, pipe}
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._

import scala.concurrent.duration._
import actorApi._
import lila.hub.actorApi.userMessage._
import lila.common.LightUser
import lila.common.PimpedJson._
import lila.hub.actorApi.activity._
import lila.hub.actorApi.chatRoom.{ChatRoomMessage, UserSubscribe}
import lila.hub.actorApi.question.{AnswerQA, CommentQA, InitQA}
import lila.hub.actorApi.relation.{GetFriendRequest, GetFriends, GetOnlineUser, ReloadOnlineFriends}
import makeTimeout.large

import scala.concurrent.Future

object Handler {

  type Controller = PartialFunction[(String, JsObject), Unit]
  type Connecter = PartialFunction[Any, (Controller, JsEnumerator, SocketMember)]

  val emptyController: Controller = PartialFunction.empty

  val AnaRateLimit = new lila.memo.RateLimit(120, 30 seconds,
    name = "socket analysis move",
    key = "socket_analysis_move")

  def apply(
    hub: lila.hub.Env,
    socket: ActorRef,
    uid: String,
    join: Any,
    userId: Option[String])(connecter: Connecter): Fu[JsSocketHandler] = {

    def baseController(member: SocketMember): Controller = {

      case ("p", _) => {
        userId.getOrElse("").length match {
          case 0 => {
            socket ! Ping(uid, 0, 0)
          }
          case _ => {
//            println("===================" + userId.get)
            (hub.actor.userMessage ? PingVersion(userId.get)) zip
              (hub.actor.relation ? PingVersion(userId.get)) map {
              case (notify: Int, makeFriend: Int) => socket ! Ping(uid, notify, makeFriend)
            }
          }
        }
      }



      case("chat", o) => {
        userId foreach { userId =>
          val obj = (o obj "d").get
          if(userId.length()> 0) hub.actor.chatRoom ! ChatRoomMessage(userId, (obj str "room").get,  (obj str "d").get)
        }
      }

      case("prevChat", o) => {
        val obj = (o obj "d").get
        socket ! GetPrevChat(uid, (obj str "v").get, (obj long "lastTime").get)
      }

      case("initChat", o) => {
        val obj = (o obj "d").get
        (obj str "t").get match {
          case "chatrooms" => {
            socket ! InitChatRoom(uid, "chatrooms", userId)
          }
          case "room" => {
            val roomId = (obj str "v").get
            userId foreach { userId =>
              if(userId.length()>0) hub.actor.chatRoom ! UserSubscribe(userId, roomId)
            }
            socket ! InitChatRoom(uid, roomId, userId)
          }
        }
      }

      case ("answer", o) => {
        val obj = (o obj "d").get
        userId foreach { userId =>
          (obj str "questionId") foreach { questionId =>
            val answer = (obj str "answer").get
            (hub.actor.question ? AnswerQA(userId, questionId, answer)) map {
              case c: JsValue => socket ! SendNewAnswer(questionId, c)
            }
          }
        }
      }

      case ("commentQA", o) => {
        val obj = (o obj "d").get
        userId foreach { userId =>
          (obj str "id") foreach { parentId =>
            val comment = (obj str "comment").get
            val questionId = (obj str "questionId").get
            val parentType = (obj str "parentType").get
            (hub.actor.question ? CommentQA(userId, parentId, parentType, comment)) map {
              case c: JsValue => socket ! SendNewCommentQA(questionId, c)
            }
          }
        }
      }

      case("comment", o) => {
        val obj = (o obj "d").get
        userId foreach { id =>
          (obj str "parent").get match {
            case "post" => {
              val postId = (obj str "id").get
              val comment = (obj str "c").get
              (hub.actor.activity ? CommentPost(id, postId, comment)) map {
                case c: JsValue => socket ! SendNewComment(postId, c)
              }
            }
            case "comment" => {
              val parentId = (obj str "id").get
              val postId = (obj str "postId").get
              val comment = (obj str "c").get
              (hub.actor.activity ? ChildCommentPost(id, postId, parentId, comment)) map {
                case c: JsValue => socket ! SendNewComment(postId, c)
              }
            }
          }
        }

      }

      case("moreComment", o) => {
        val obj = (o obj "d").get
        val postId = (obj str "id").get
        val time = (obj long "time").get
        (hub.actor.activity ? MoreCommentPost(postId, time)) map {
          case c: JsValue => socket ! SendMoreComment(uid, postId, c)
        }
      }

      case("sub", o) => {
        val obj = (o obj "d").get
        (obj str "t").get match {
          case "chatrooms" => {
            socket ! Sub(uid, "chatrooms", userId)
            userId foreach { userId =>
              if(userId.length() > 0) hub.actor.chatRoom ! UserSubscribe(userId, "chatrooms")
            }
          }
          case "room" => {
            val roomId = (obj str "v").get
            socket ! Sub(uid, roomId, userId)
            userId foreach { userId =>
              if(userId.length()>0) hub.actor.chatRoom ! UserSubscribe(userId, roomId)
            }
          }
        }
      }

      case("morePost", o) => {
        val obj = (o obj "d").get
        val time = (obj long "time").get
        (hub.actor.activity ? MorePost(userId.get, time)) foreach {
          case posts: JsValue => {
            socket ! SendMorePost(uid, posts)
          }
        }
      }

      case ("subPost", o) => {
        val obj = (o obj "d").get
        (obj str "id") foreach { postId =>
          socket ! SubPost(uid, postId)
        }
      }

      case ("subQuestion", o) => {
        val obj = (o obj "d").get
        (obj str "id") foreach { questionId =>
          socket ! SubQuestion(uid, questionId)
        }
      }

      case ("unSubPost", o) => socket ! UnSubPost(uid)

      case ("unSubQuestion", o) => socket ! UnSubQuestion(uid)


      case("unSub", o) => {
        ((o obj "d").get str "t").get match {
          case "chatrooms" => {
            socket ! UnSub(uid, "chatrooms")
          }
          case "room" => socket ! UnSub(uid, ((o obj "d").get str "v").get)

        }
      }

      case ("get_onlines", _) => userId foreach { u =>
        (hub.actor.relation ? GetOnlineUser(u)) foreach {
          case data: List[LightUser] =>
            socket ! SendOnlineFriends(uid, data)
        }
      }

      case ("getAllFriends", _) => userId foreach { u =>
        (hub.actor.relation ? GetFriends(u)) foreach {
          case data: List[LightUser] =>
            socket ! SendFriendsList(uid, data)
        }
      }

      case ("gn", o) => userId foreach { u =>
        val id = (o\"d").as[String]
        (hub.actor.userMessage ? GetName(id)) foreach {
          case "error" => //println("errror:" +id)
          case name:String  => socket ! SendName(uid, id, name)
        }
      }

      case ("gmm", o) => userId foreach { u =>
        if(u.length > 0){
          val f = ((o\"d").as[JsObject]\"f").as[Int]
          val t = ((o\"d").as[JsObject]\"t").as[Int]
          (hub.actor.userMessage ? MissingMes(u, f, t)) foreach {
            case dataFu: Future[List[JsValue]] => dataFu.map{
              data => socket ! SendMissingMes(uid, f, t, data)
            }
          }
        }
      }


      case ("mr", o) => userId foreach { userId =>
        if(userId.length > 0){
          val toId = (o\"d"\"uid").as[String]
          val mv = (o\"d"\"mv").as[Int]
          hub.actor.userMessage ! MarkRead(userId, toId, mv)
        }
      }

      case ("initPost", o) => userId foreach { id =>
        if(id.length() > 0){
          (hub.actor.activity ? InitPost(id)) foreach {
            case posts: JsValue => {
              socket ! SendInitPost(uid, posts)
            }
          }
        }
      }

      case ("initQA", o) =>
        (hub.actor.question ? InitQA(userId)) foreach {
          case posts: JsValue => {
            socket ! SendInitQA(uid, posts)
          }
        }

      case ("init_chat", o) => userId foreach { fromId =>
        if(fromId.length() > 0) {
          (hub.actor.userMessage ? InitChat(fromId, (o \ "d" \ "w").as[String], (o \ "d" \ "cv").as[Int])) foreach{
            case dataFu: Future[List[JsValue]] => dataFu.map{
              case data => {
                socket ! SendInitMes(uid, data)
              }
            }
          }
        }
      }

      case ("gnm", o) => userId foreach { userId =>
        if(userId.length() > 0) {
          (hub.actor.userMessage ? InitNotify(userId)) foreach {
            case dataFu: Future[List[JsValue]] => dataFu.map{
              case data => socket ! SendInitNotify(uid, data)
            }

          }
        }
      }

      case ("gmf", o) => userId foreach { userId =>
        if(userId.length() > 0) {
          (hub.actor.relation ? GetFriendRequest(userId)) foreach {
            case fuData:Future[Set[LightUser]] => {
              fuData.map{
                data => socket ! SendFriendRequest(uid, data)
              }
            }
          }
        }
      }

      case ("m", o) => userId foreach { u =>
        if(u.length() > 0 ) {
          (o \ "d").asOpt[JsObject] foreach { data =>
            if(((o obj "d").get str "to").get != u) hub.actor.userMessage ! Msg(u, data)
          }
        }
      }

      case ("following_onlines", _) => userId foreach { u =>
        hub.actor.relation ! ReloadOnlineFriends(u)
      }

//      case ("test", o) => {
//        val mes = (o\"d").as[JsObject]
//        mes str "to" foreach { to =>
//          mes str "mes" foreach { mes =>
//            socket ! Test2(uid, to, mes)
//          }
//        }
//      }
    }

    def iteratee(controller: Controller, member: SocketMember): JsIteratee = {
      val control = controller orElse baseController(member)
      Iteratee.foreach[JsValue](jsv =>
        jsv.asOpt[JsObject] foreach { obj =>
          obj str "t" foreach { t =>
            control.lift(t -> obj)
          }
        }
      ).map(_ => socket ! Quit(uid))
    }

    socket ? join map connecter map {
      case (controller, enum, member) => iteratee(controller, member) -> enum
    }
  }
}
