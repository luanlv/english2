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

object PostRepo {

  private lazy val coll = Env.current.postColl

  def insert(id: String, userId: String, content: String): Future[WriteResult] = {
    val post = Post(id, content, lila.user.Env.current.lightUserApi.get(userId).get)
    coll.insert(post)
  }

  def getOnePost(userId: String, postId: String) = {
    coll.find(BSONDocument("_id" -> postId),
      BSONDocument(
        "_id" -> 1,
        "content" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "likeCount" -> 1,
        "likes" -> BSONDocument("$elemMatch" -> BSONDocument("$eq" -> userId)),
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1
      )
    )
    .cursor[Post]()
    .headOption
  }

  def newComment(postId: String) = {
    coll.update(
      BSONDocument("_id" -> postId),
      BSONDocument(
        "$inc" -> BSONDocument("commentCount" -> 1)
      )
    )
  }

  def getPost(userId: String, ids: Set[ID], timepoint: DateTime): Fu[List[Post]] = {
    coll.find(BSONDocument("userId" -> BSONDocument("$in" -> ids)),
      BSONDocument(
        "_id" -> 1,
        "content" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "likeCount" -> 1,
        "likes" -> BSONDocument("$elemMatch" -> BSONDocument("$eq" -> userId)),
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1
      )
    )
      .sort(BSONDocument("published" -> -1))
      .cursor[Post]()
      .gather[List](10)
  }

  def getMorePost(userId: String, ids: Set[ID], timepoint: DateTime): Fu[List[Post]] = {
    coll.find(BSONDocument("userId" -> BSONDocument("$in" -> ids), "published" -> BSONDocument("$lt" -> BSONDateTime(timepoint.getMillis))),
      BSONDocument(
        "_id" -> 1,
        "content" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "likeCount" -> 1,
        "likes" -> BSONDocument("$elemMatch" -> BSONDocument("$eq" -> userId)),
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1
      )
    )
      .sort(BSONDocument("published" -> -1))
      .cursor[Post]()
      .gather[List](10)
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