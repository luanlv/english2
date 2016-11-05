package lila.question

import java.util.UUID

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
//import lila.question.actorApi._
import lila.hub.actorApi.question._
import lila.hub.actorApi.userMessage.PingVersion
import org.joda.time.DateTime
import play.api.libs.json.Json

//import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.question._
import lila.hub.actorApi.{ SendTo, SendTos }
import makeTimeout.short

private[question] final class QuestionActor(
                                             questionApi: QuestionApi,
                                             answerApi: AnswerApi,
                                             commentApi: CommentApi
                                           ) extends Actor {


  private val bus = context.system.lilaBus

  def receive = {
    case InitQA(userId) => {
      sender ! Json.toJson(questionApi.getQuestion(userId, DateTime.now()).await)
    }

    case CommentQA(userId, parentId, parentType, comment) => {
      sender ! Json.toJson(addComment(userId, parentId, parentType, comment))
    }

    case AnswerQA(userId, questionId, answer) => sender ! Json.toJson(answerApi.newAnswer(userId, questionId, answer).await)

  }

  def addComment(userId: String, parentId: String, parentType: String, comment: String) = {
    val opComment = commentApi.newComment(userId, parentId, parentType, comment).await
    opComment foreach {
      comment =>  {
        if(parentType == "q"){
          questionApi.addComment(parentId, comment)
        } else if (parentType == "a") {
          answerApi.addComment(parentId, comment)
        }
      }
    }
    opComment
  }
}
