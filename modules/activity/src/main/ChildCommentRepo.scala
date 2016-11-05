package lila.activity

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
import lila.db.dsl._

object ChildCommentRepo {

  private lazy val coll = Env.current.childCommentColl

  def insert(id: String, parentId: String, userId: String,  comment: String, time: DateTime): Future[WriteResult] = {
    //    val info = Info(0, List(), 0, List(), 0)
    val newComment = ChildComment(id, parentId, comment, lila.user.Env.current.lightUserApi.get(userId).get, time, 0, Option(List()))
    coll.insert(newComment)
  }

  def getOneComment(userId: String, commentId: String):Fu[Option[ChildComment]] = {
    coll.find(BSONDocument("_id" -> commentId),
      BSONDocument(
        "_id" -> 1,
        "parentId" -> 1,
        "comment" -> 1,
        "userId" -> 1,
        "time" -> 1,
        "likeCount" -> 1,
        "likes" -> BSONDocument("$elemMatch" -> BSONDocument("$eq" -> userId))
      )
    )
      .cursor[ChildComment]()
      .headOption
  }

  def getComment(userId: String, postId: String, timepoint: DateTime, nb: Int): Fu[List[Comment]] = {
    coll.find(BSONDocument("parentPost" -> postId),
      BSONDocument(
        "_id" -> 1,
        "parentId" -> 1,
        "comment" -> 1,
        "userId" -> 1,
        "time" -> 1,
        "likeCount" -> 1,
        "likes" -> BSONDocument("$elemMatch" -> BSONDocument("$eq" -> userId)),
        "childCount" -> 1,
        "children" -> 1
      )
    )
      .sort(BSONDocument("time" -> -1))
      .cursor[Comment]()
      .gather[List](nb)
  }

  def like(userId: String, postId: String) = {
    coll.update(
      BSONDocument("_id" -> postId, "likes" -> BSONDocument("$ne" -> userId)),
      BSONDocument(
        "$inc" -> BSONDocument("likeCount" -> 1),
        "$push" -> BSONDocument("likes" -> userId)
      )
    ).void
  }

  def unlike(userId: String, postId: String) = {
    coll.update(
      BSONDocument("_id" -> postId, "likes" -> userId),
      BSONDocument(
        "$inc" -> BSONDocument("likeCount" -> -1),
        "$pull" -> BSONDocument("likes" -> userId)
      )
    ).void
  }

}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]