package lila.activity

import java.util.UUID

import akka.actor.ActorSelection
import lila.activity.actorApi._
import lila.common.LightUser
import org.joda.time.DateTime
import scala.util.Success

import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{ Propagate, Follow => FollowUser }
//import lila.usrMessage.MessageRepo

final class ChildCommentApi(
                        cached: Cached,
                        actor: ActorSelection) {

  private def counter = lila.counter.Env.current.api

  def getComment(userId: String, postId: String, timepoint: DateTime, nb:Int) = CommentRepo.getComment(userId, postId, timepoint, nb)

  def newChildComment(userId: String, postId: String, parentId: String, comment: String) = {
    val commentId = counter.getNextId("childComment").toString
    ChildCommentRepo.insert(commentId, parentId, userId, comment, DateTime.now())  >> getOneComment(userId, commentId) >>-  CommentRepo.newChildComment(parentId) >>-  PostRepo.newComment(postId)
  }

  def getOneComment(userId: String, commentId: String) = {
    ChildCommentRepo.getOneComment(userId, commentId)
  }

  def like(userId: String, postId: String) = {
    PostRepo.like(userId, postId)
  }

  def unlike(userId: String, postId: String) = {
    PostRepo.unlike(userId, postId)
  }

  def pushPost(userId: String, postId: String) = {
    actor ! NewPost(userId, postId)
  }

  def pushComment(postId: String) = {
    actor ! PushComment(postId)
  }
}
