package lila.question

import java.util.UUID

import akka.actor.ActorSelection
//import lila.activity.actorApi._
import lila.common.LightUser
import org.joda.time.DateTime
import scala.util.Success
import lila.db.dsl._

import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
//import lila.usrMessage.MessageRepo

final class AnswerApi(
                         cached: Cached,
                         actor: ActorSelection) {

  private def counter = lila.counter.Env.current.api

  def newAnswer(userId: String, questionId: String, answer: String) = {
    val answerId = counter.getNextId("answer").toString
    AnswerRepo.insert(answerId, userId, questionId, answer)  >> getOneAnswer(userId, answerId)
  }

  def getOneAnswer(userId: String, answerId: String) = {
    AnswerRepo.getOneAnswer(userId, answerId)
  }

  def addComment(parentId: String, comment: Comment) = {
    AnswerRepo.addComment(parentId, comment)
  }

  def voteUp(userId: String, answerId: String) = {
    AnswerRepo.voteUp(userId, answerId)
  }

  def voteDown(userId: String, answerId: String) = {
    AnswerRepo.voteDown(userId, answerId)
  }

  def revoteUp(userId: String, answerId: String) = {
    AnswerRepo.revoteUp(userId, answerId)
  }

  def revoteDown(userId: String, answerId: String) = {
    AnswerRepo.revoteDown(userId, answerId)
  }

//  def getOneQuestion(userId: String, answerId: String) = {
//    QuestionRepo.getOneQuestion(userId, answerId)
//  }

  def getAnswer(userId: Option[String], questionId: String, timepoint: DateTime) = {
    userId match {
      case None => AnswerRepo.getAnswer(questionId, timepoint)
      case Some(uid) => AnswerRepo.getAnswer(uid, questionId, timepoint)
    }
  }
}
