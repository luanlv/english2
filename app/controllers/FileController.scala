package controllers

import java.io.File

import lila.app._
import lila.user.{Cached, UserRepo, User => UserModel}
import play.api._
import play.api.data.Form
import play.api.data.Forms._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Promise
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import play.api.mvc._
import views.html.helper.form

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext


object FileController extends LilaController{

  def getFile(name: String) = Action {
    val file = new File("_file/" + name)
    Ok.sendFile(
      file,
      inline = true
    ).withHeaders("Cache-Control" -> "no-cache, no-store, must-revalidate")
  }


}
