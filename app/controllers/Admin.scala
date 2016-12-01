package controllers

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc._
import play.twirl.api.Html
import lila.api.Context
import lila.app._
import lila.relation.Related
import lila.user.{UserRepo, User => UserModel}
import views._


object Admin extends LilaController {

  private def env = Env.vocab.api

  def newQuestion = OpenBody(BodyParsers.parse.tolerantJson) { implicit ctx =>
    val req = ctx.body
    val question = Json.parse(req.body.toString).as[JsObject]
    env.newQuestion(question) map {
      result =>
        if(result >=0 ) {
          Ok(result)
        } else {
          BadRequest
        }
    }
  }

  def getQuestion(id: Int) = Open { implicit ctx =>
    env.getQuestion(id) map {
      opQuestion => opQuestion match {
        case Some(question) => Ok(Json.toJson(question))
        case None => BadRequest
      }
    }
  }

}
