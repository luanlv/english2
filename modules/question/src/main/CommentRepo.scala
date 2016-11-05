package lila.question

import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._

import spray.caching.{ LruCache, Cache }

import lila.common.LightUser
import BSONHandlers._
import lila.db.BSON._
import scala.concurrent.ExecutionContext.Implicits.global


object CommentRepo {

  private lazy val coll = Env.current.commentQAColl

  def insert(id: String, userId: String, parentId: String, comment: String): Future[WriteResult] = {
    val newComment = Comment(id, parentId, comment, lila.user.Env.current.lightUserApi.get(userId).get)
    coll.insert(newComment)
  }

  def getOneComment(userId: String, commentId: String):Fu[Option[Comment]] = {
    coll.find(BSONDocument("_id" -> commentId))
      .cursor[Comment]()
      .headOption
  }

}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]