package controllers

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

object Chat extends LilaController {
  private def env = Env.security
  private def api = env.api

  def chatRooms = Open { implicit ctx =>
    Ok(views.html.index.home()).fuccess
  }

  def chatRoom(id: String) = Open { implicit ctx =>
    implicit val req = ctx.req
    Ok(views.html.index.home()).fuccess
  }

}
