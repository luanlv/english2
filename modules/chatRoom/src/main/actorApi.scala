package lila.chatRoom
package actorApi

import lila.common.LightUser

private[chatRoom] case class AllOnlineFriends(onlines: Map[ID, LightUser])
private[chatRoom] case object NotifyMovement
