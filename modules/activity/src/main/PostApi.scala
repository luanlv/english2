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

final class PostApi(
                 cached: Cached,
                 actor: ActorSelection) {

  private def counter = lila.counter.Env.current.api

  def getPost(userId: String, ids: Set[String], timepoint: DateTime) = PostRepo.getPost(userId, ids, timepoint)

  def newPost(userId: String, content: String) = {
    val postId = counter.getNextId("post").toString
    PostRepo.insert(postId, userId, content) >>- pushPost(userId, postId)
  }

  def getOnePost(userId: String, postId: String) = {
    PostRepo.getOnePost(userId, postId)
  }

  def getMorePost(userId: String, ids: Set[String], timepoint: DateTime) = PostRepo.getMorePost(userId, ids, timepoint)

  def like(userId: String, postId: String) = {
    PostRepo.like(userId, postId)
  }

  def unlike(userId: String, postId: String) = {
    PostRepo.unlike(userId, postId)
  }

  def pushPost(userId: String, postId: String) = {
    actor ! NewPost(userId, postId)
  }
}
