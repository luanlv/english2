package controllers

import lila.activity.PostRepo
import org.joda.time.DateTime
import play.api.data._, Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._, Results._
import play.api.Play.current

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import lila.user.{ UserRepo, User => UserModel }
import views._
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global


object API extends LilaController {

  private def env = Env.user
  private def relationApi = Env.relation.api
  private def postApi = Env.activity.postApi
  private def commentApi = Env.activity.commentApi
  private def questionApi = Env.question.questionApi
  private def answerApi = Env.question.answerApi

  def getSelfInformation = Auth { implicit ctx =>
    me => {
      Ok(Json.obj("username" -> me.username, "name" -> me.name, "avatar" -> me.avatar)).fuccess
    }
  }

  def updateInformation =  OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
    val req = ctx.body
    val name = (Json.parse(req.body.toString).as[JsObject]\"name").as[String]
    UserRepo.updateName(ctx.userId.get, name) map {
      lila.user.Env.current.lightUserApi.refresh(ctx.userId.get)
      result => Ok("")
    }
  }

  def getInformationUser(username: String) = Open { implicit ctx =>
    OptionFuResult(UserRepo named username) { user =>

      (ctx.userId ?? { relationApi.fetchBlocks(user.id, _) }) zip
        (ctx.isAuth ?? { Env.pref.api.followable(user.id) }) zip
        (ctx.userId ?? { relationApi.fetchRelation(_, user.id) }) map {
        case ((blocked, followable), relation) =>
          Ok(Json.obj(
            "username" -> user.username,
            "name" -> user.name,
            "avatar" -> user.avatar,
            "extra" -> Json.obj(
              "block" -> blocked,
              "followable" -> followable,
              "relation" -> relation
              )
            )
        )
      }
    }
  }

  def getListFollower(username: String) = Open { implicit  ctx =>
    relationApi.fetchFollowers(username).map {
      listPeople => Ok(Json.toJson(listPeople))
    }
  }

  def getListFriend(username: String) = Open { implicit  ctx =>
    relationApi.fetchFriends(username).map {
      listPeople => Ok(Json.toJson(listPeople))
    }
  }

  def doPost =  OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
    val req = ctx.body
    val content = ((Json.parse(req.body.toString).as[JsObject]\"content").as[String]).replaceAll("\\n\\n\\s*\\n", "\n\n").replaceAll("[ \\t\\x0B\\f]+", " ").trim()
    ctx.userId match {
      case Some(id) => {
        postApi.newPost(id, content) map {
          writeResult =>
            if(writeResult.ok){
              Ok(Json.obj("action" -> "ok"))
            } else {
              Ok(Json.obj("action" -> "error"))
            }
        }
      }
      case None => BadRequest.fuccess
    }
  }

  //  def doAnswer =  OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
  //    val req = ctx.body
  //    println(Json.parse(req.body.toString).as[JsObject])
  //    val questionId = ((Json.parse(req.body.toString).as[JsObject]\"questionId").as[String])
  //    val answer = ((Json.parse(req.body.toString).as[JsObject]\"answer").as[String]).replaceAll("\\n\\n\\s*\\n", "\n\n").replaceAll("[ \\t\\x0B\\f]+", " ").trim()
  //    ctx.userId match {
  //      case Some(uid) => {
  //        answerApi.newAnswer(uid, questionId, answer) map {
  //          writeResult =>
  //            if(writeResult.ok){
  //              Ok(Json.obj("action" -> "ok"))
  //            } else {
  //              Ok(Json.obj("action" -> "error"))
  //            }
  //        }
  //      }
  //      case None => BadRequest.fuccess
  //    }
  //  }
  def newQuestion = Open { implicit ctx =>
    questionApi.getNewQuestion.map {
      listQuestion => Ok(Json.toJson(listQuestion))
    }
  }

  def hotQuestion = Open { implicit ctx =>
    questionApi.getHotQuestion.map {
      listQuestion => Ok(Json.toJson(listQuestion))
    }
  }

  def doAsk =  OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
    val req = ctx.body
    val question = ((Json.parse(req.body.toString).as[JsObject]\"question").as[String])
    val description = ((Json.parse(req.body.toString).as[JsObject]\"description").as[String]).replaceAll("\\n\\n\\s*\\n", "\n\n").replaceAll("[ \\t\\x0B\\f]+", " ").trim()
    ctx.userId match {
      case Some(uid) => {
        questionApi.newQuestion(uid, question, description) map {
          writeResult =>
            if(writeResult.ok){
              Ok(Json.obj("action" -> "ok"))
            } else {
              Ok(Json.obj("action" -> "error"))
            }
        }
      }
      case None => BadRequest.fuccess
    }
  }

  def getQuestion(questionId: String) = Open { implicit  ctx =>
    Ok(views.html.index.home()).fuccess
  }

  def getAllQuestion = Open { implicit ctx =>
    questionApi.getQuestion(ctx.userId, DateTime.now()).map {
      listQuestion => Ok(Json.toJson(listQuestion))
    }
  }



  def getPost(username: String) = Open { implicit ctx =>
    if(ctx.userId.get == username) {
      val listFriend = relationApi.fetchFriends(username).await
      val listUser = listFriend.+(username).+("admin")
      //      println(listUser)
      val fuPost = PostRepo.getPost(ctx.userId.get, listUser, DateTime.now())
      fuPost map {
        posts => Ok(Json.toJson(posts))
      }
    } else {
      val fuPost = PostRepo.getPost(ctx.userId.get, Set(username), DateTime.now())
      fuPost map {
        posts => Ok(Json.toJson(posts))
      }
    }
  }


  def viewPost(postId: String) = Open { implicit ctx =>
    (ctx.userId ?? { postApi.getOnePost( _ , postId) }) zip
      (ctx.userId ?? { commentApi.getComment(_, postId, DateTime.now(), 4) })  map {
      case (post, comment) =>
        Ok(Json.obj("post" -> post, "comment" -> comment))
    }
  }

  def viewQuestion(questionId: String) = Open {implicit ctx =>
    questionApi.getOneQuestion( ctx.userId , questionId) zip
      answerApi.getAnswer( ctx.userId , questionId, DateTime.now()) map {
      case (question, answers) => Ok(Json.obj("question" -> question, "answer" -> answers))
    }
  }

  def likePost(postId: String) = Open { implicit ctx =>
    ctx.userId match {
      case None => BadRequest.fuccess
      case Some(id) => postApi.like(id, postId) map (_ => Ok("liked"))
    }
  }

  def vote(questionId: String) =  OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
    val req = ctx.body
    val kind = ((Json.parse(req.body.toString).as[JsObject]\"vote").as[String])

    ctx.userId match {
      case Some(uid) => {
        kind match {
          case "up" => questionApi.voteUp(uid, questionId) map (_ => Ok("voted"))
          case "down" => questionApi.voteDown(uid, questionId) map (_ => Ok("voted"))
          case "reup" => questionApi.revoteUp(uid, questionId) map (_ => Ok("voted"))
          case "redown" => questionApi.revoteDown(uid, questionId) map (_ => Ok("voted"))
          case _ => BadRequest.fuccess
        }
      }
      case None => BadRequest.fuccess
    }
  }

  def voteAnswer(answerId: String) =  OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
    val req = ctx.body
    val kind = ((Json.parse(req.body.toString).as[JsObject]\"vote").as[String])

    ctx.userId match {
      case Some(uid) => {
        kind match {
          case "up" => answerApi.voteUp(uid, answerId) map (_ => Ok("voted"))
          case "down" => answerApi.voteDown(uid, answerId) map (_ => Ok("voted"))
          case "reup" => answerApi.revoteUp(uid, answerId) map (_ => Ok("voted"))
          case "redown" => answerApi.revoteDown(uid, answerId) map (_ => Ok("voted"))
          case _ => BadRequest.fuccess
        }
      }
      case None => BadRequest.fuccess
    }
  }


  def unlikePost(postId: String) = Open { implicit ctx =>
    ctx.userId match {
      case None => BadRequest.fuccess
      case Some(id) => postApi.unlike(id, postId) map (_ => Ok("unliked"))
    }
  }

}
