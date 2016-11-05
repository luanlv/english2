package controllers

import lila.app._
import lila.hub.actorApi.{GetUserIds, GetUids}
import lila.user.{ Cached, UserRepo, User => UserModel }
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Promise
import play.api.libs.json.{JsString, JsArray, JsObject, Json}
import play.api.mvc._
import views.html.helper.form
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration._


import com.ybrikman.ping.javaapi.bigpipe.PageletRenderOptions
import com.ybrikman.ping.scalaapi.bigpipe._
import com.ybrikman.ping.scalaapi.bigpipe.HtmlStreamImplicits._

import com.ybrikman.ping.javaapi.bigpipe.PageletRenderOptions._
import scala.concurrent.ExecutionContext


object Application extends LilaController{

  private def env = lila.app.Env.current

  def index = Open { implicit  ctx =>
    Ok(views.html.index.home()).fuccess
  }
  def post(postId: String) = Open { implicit  ctx =>
    Ok(views.html.index.home()).fuccess
  }

  def json = Open { implicit ctx =>
    val fuJson = Future(Json.obj("data" -> "data recieved from sever!!!"))
    val fuJsonDelay:Future[JsObject] =  Promise.timeout(fuJson, 0 second).flatMap(x => x)
    fuJsonDelay.map{
      data => Ok(data)
    }
  }

  def user(user: String) = Open {implicit ctx =>
    Ok(views.html.index.home()).fuccess
  }

  def userMini(user: String) = Open {implicit  ctx =>
    lila.app.Env.user.lightUserApi.get(user) match {
      case None => Ok("").fuccess
      case Some(lightUser) => {
        //        val fuData = Future("<div>" + lightUser.name + "</div>")
        //        val result = Promise.timeout(fuData, 0.2 second).flatMap(x => x)
        //        result.map {
        //          x => Ok(x)
        //        }
        Ok("<div>" + lightUser.name + "</div>").fuccess
      }
    }
  }

}
