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

final class CommentApi(
                         cached: Cached,
                         actor: ActorSelection) {

  def newComment(userId: String, parentId: String, parentType: String, comment: String) = {
    val commentId = UUID.randomUUID().toString
    CommentRepo.insert(commentId, userId, parentId, comment) >> CommentRepo.getOneComment(userId, commentId)
  }
}
