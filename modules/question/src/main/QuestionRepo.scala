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

object QuestionRepo {

  private lazy val coll = Env.current.questionColl

  def insert(id: String, userId: String, question: String, description: String): Future[WriteResult] = {
    val newQuestion = Question(id, question, description, lila.user.Env.current.lightUserApi.get(userId).get)
    coll.insert(newQuestion)
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

  def newAnswer(questionId: String) = {
    coll.update(
      BSONDocument("_id" -> questionId),
      BSONDocument(
        "$inc" -> BSONDocument("answerCount" -> 1)
      )
    )
  }

  def getOneQuestion(questionId: String) = {
    coll.find(BSONDocument("_id" -> questionId),
      BSONDocument(
        "_id" -> 1,
        "question" -> 1,
        "description" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "views" -> 1,
        "voteCount" -> 1,
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1,
        "comment" -> 1,
        "answerCount" -> 1
      )
    )
      .cursor[Question]()
      .headOption
  }

  def getOneQuestion(userId: String, questionId: String) = {
    println(userId)
    coll.find(BSONDocument("_id" -> questionId),
      BSONDocument(
        "_id" -> 1,
        "question" -> 1,
        "description" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "views" -> 1,
        "voteCount" -> 1,
        "votes" -> BSONDocument("$elemMatch" -> BSONDocument("userId"  ->  userId)),
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1,
        "comment" -> 1,
        "answerCount" -> 1
      )
    )
      .cursor[Question]()
      .headOption
  }

  def voteUp(userId: String, questionId: String) = {
    coll.update(BSONDocument("_id" -> questionId, "votes.userId" -> BSONDocument("$ne" -> userId)),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> 1),
        "$push" -> BSONDocument("votes" -> BSONDocument("userId" -> userId, "vote" -> 1))
      )
    ).void
  }

  def voteDown(userId: String, questionId: String) = {
    coll.update(BSONDocument("_id" -> questionId, "votes.userId" -> BSONDocument("$ne" -> userId)),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> -1),
        "$push" -> BSONDocument("votes" -> BSONDocument("userId" -> userId, "vote" -> -1))
      )
    ).void
  }

  def revoteUp(userId: String, questionId: String) = {
    coll.update(BSONDocument("_id" -> questionId, "votes.userId" -> userId, "votes.vote" -> -1),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> 2),
        "$set" -> BSONDocument("votes.$.vote" -> 1)
      )
    ).void
  }

  def revoteDown(userId: String, questionId: String) = {
    coll.update(BSONDocument("_id" -> questionId, "votes.userId" -> userId, "votes.vote" -> 1),
      BSONDocument(
        "$inc" -> BSONDocument("voteCount" -> -2),
        "$set" -> BSONDocument("votes.$.vote" -> -1)
      )
    ).void
  }

  def getQuestion(timepoint: DateTime): Fu[List[Question]] = {
    coll.find(BSONDocument(),
      BSONDocument(
        "_id" -> 1,
        "question" -> 1,
        "description" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "views" -> 1,
        "voteCount" -> 1,
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1,
        "answerCount" -> 1
      )
    )
      .sort(BSONDocument("published" -> -1))
      .cursor[Question]()
      .gather[List](10)
  }

  def getNewQuestion: Fu[List[Question]] = {
    coll.find(BSONDocument())
      .sort(BSONDocument("published" -> -1))
      .cursor[Question]()
      .gather[List](10)
  }

  def getHotQuestion: Fu[List[Question]] = {
    coll.find(BSONDocument())
      .sort(BSONDocument("voteCount" -> -1))
      .cursor[Question]()
      .gather[List](10)
  }

  def getQuestion(userId: String, timepoint: DateTime): Fu[List[Question]] = {
    coll.find(BSONDocument(),
      BSONDocument(
        "_id" -> 1,
        "question" -> 1,
        "description" -> 1,
        "userId" -> 1,
        "published" -> 1,
        "views" -> 1,
        "voteCount" -> 1,
        "votes" -> BSONDocument("$elemMatch" -> BSONDocument("userId"  ->  userId)),
        "shareCount" -> 1,
        "shares" -> 1,
        "commentCount" -> 1,
        "answerCount" -> 1
      )
    )
      .sort(BSONDocument("published" -> -1))
      .cursor[Question]()
      .gather[List](10)
  }

}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]