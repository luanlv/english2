package lila.activity
package actorApi

import lila.common.LightUser

private[activity] case class NewPost(user: String, postId: String)
private[activity] case class PushComment(postId: String)
