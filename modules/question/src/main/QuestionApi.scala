package lila.question

import java.util.UUID

import akka.actor.ActorSelection
//import lila.activity.actorApi._
import lila.common.LightUser
import org.joda.time.DateTime
import scala.util.Success

import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
//import lila.usrMessage.MessageRepo

final class QuestionApi(
                     cached: Cached,
                     actor: ActorSelection) {

  private def counter = lila.counter.Env.current.api

  def newQuestion(userId: String, question: String, description: String) = {
    val questionId = counter.getNextId("answer").toString
    QuestionRepo.insert(questionId, userId, question, description) // >>- pushPost(userId, postId)
  }

  def addComment(parentId: String, comment: Comment) = {
    QuestionRepo.addComment(parentId, comment)
  }

  def voteUp(userId: String, questionId: String) = {
    QuestionRepo.voteUp(userId, questionId)
  }

  def voteDown(userId: String, questionId: String) = {
    QuestionRepo.voteDown(userId, questionId)
  }

  def revoteUp(userId: String, questionId: String) = {
    QuestionRepo.revoteUp(userId, questionId)
  }

  def revoteDown(userId: String, questionId: String) = {
    QuestionRepo.revoteDown(userId, questionId)
  }

  def getOneQuestion(userId: Option[String], questionId: String) = {
    userId match {
      case None => QuestionRepo.getOneQuestion(questionId)
      case Some(uid) => QuestionRepo.getOneQuestion(uid, questionId)
    }
  }

  def getQuestion(userId: Option[String], timepoint: DateTime) = {
    userId match {
      case None => QuestionRepo.getQuestion(timepoint)
      case Some(uid) => QuestionRepo.getQuestion(uid, timepoint)
    }
  }

  def getNewQuestion = {
    QuestionRepo.getNewQuestion
  }

  def getHotQuestion = {
    QuestionRepo.getHotQuestion
  }
}
