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
import lila.db.dsl._

object AnswerRepo {

  private lazy val coll = Env.current.answerColl

  def insert(id: String, userId: String, questionId: String, answer: String): Future[WriteResult] = {
    val newAnswer = Answer(id, questionId, answer, lila.user.Env.current.lightUserApi.get(userId).get)
    coll.insert(newAnswer) >>- QuestionRepo.newAnswer(questionId)
  }

  def addComment(parentId: String, comment: Comment) = {
    //    val childComment = BSONFormats.toBSON(Json.toJson(comment)).get.asInstanceOf[BSONDocument]
    coll.update(
      BSONDocument("_id" -> parentId),
      BSONDocument(
        "$push" -> BSONDocument("comment" -> BSONDocument(
          "$each" -> BSONArray(comment),
          "$slice" -> -4
        )
        )
      )
    )
  }

  def getOneAnswer(userId: String, answerId: String) = {
    coll.find(BSONDocument("_id" -> answerId),
      BSONDocument(
        "_id" -> 1,
        "qId" -> 1,
        "answer" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "voteCount" -> 1,
        "votes" -> BSONDocument("$elemMatch" -> BSONDocument("userId"  ->  userId)),
        "commentCount" -> 1,
        "comment" -> 1
      )
    )
      .cursor[Answer]()
      .headOption
  }

  def voteUp(userId: String, answerId: String) = {
    coll.update(BSONDocument("_id" -> answerId, "votes.userId" -> BSONDocument("$ne" -> userId)),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> 1),
        "$push" -> BSONDocument("votes" -> BSONDocument("userId" -> userId, "vote" -> 1))
      )
    ).void
  }

  def voteDown(userId: String, answerId: String) = {
    coll.update(BSONDocument("_id" -> answerId, "votes.userId" -> BSONDocument("$ne" -> userId)),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> -1),
        "$push" -> BSONDocument("votes" -> BSONDocument("userId" -> userId, "vote" -> -1))
      )
    ).void
  }

  def revoteUp(userId: String, answerId: String) = {
    coll.update(BSONDocument("_id" -> answerId, "votes.userId" -> userId, "votes.vote" -> -1),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> 2),
        "$set" -> BSONDocument("votes.$.vote" -> 1)
      )
    ).void
  }

  def revoteDown(userId: String, answerId: String) = {
    coll.update(BSONDocument("_id" -> answerId, "votes.userId" -> userId, "votes.vote" -> 1),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> -2),
        "$set" -> BSONDocument("votes.$.vote" -> -1)
      )
    ).void
  }

  def getAnswer(questionId: String, timepoint: DateTime): Fu[List[Answer]] = {
    coll.find(BSONDocument("qId" -> questionId),
      BSONDocument(
        "_id" -> 1,
        "qId" -> 1,
        "answer" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "voteCount" -> 1,
        "commentCount" -> 1,
        "comment" -> 1
      )
    )
      .sort(BSONDocument("published" -> -1))
      .cursor[Answer]()
      .gather[List](10)
  }


  def getAnswer(userId: String, questionId: String, timepoint: DateTime): Fu[List[Answer]] = {
    coll.find(BSONDocument("qId" -> questionId),
      BSONDocument(
        "_id" -> 1,
        "qId" -> 1,
        "answer" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "voteCount" -> 1,
        "votes" -> BSONDocument("$elemMatch" -> BSONDocument("userId"  ->  userId)),
        "commentCount" -> 1,
        "comment" -> 1
      )
    )
      .sort(BSONDocument("published" -> -1))
      .cursor[Answer]()
      .gather[List](10)
  }
}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]