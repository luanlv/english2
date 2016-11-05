package lila.userMessage
package actorApi

import lila.common.LightUser

private[userMessage] case class AllOnlineFriends(onlines: Map[ID, LightUser])
private[userMessage] case object NotifyMovement
