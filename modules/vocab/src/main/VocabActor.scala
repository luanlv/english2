package lila.vocab

import akka.actor.{ Actor, ActorSelection }
import akka.pattern.{ ask, pipe }
import lila.hub.actorApi.userMessage._
import org.joda.time.DateTime
import play.api.libs.json._

import actorApi._


import lila.common.LightUser
import lila.hub.actorApi.relation._
import lila.hub.actorApi.{SendTos, SendTo}
import makeTimeout.short
import reactivemongo.bson.BSONDocument

private[vocab] final class VocabActor(
                                               getOnlineUserIds: () => Set[String],
                                               lightUser: String => Option[LightUser],
                                               api: Api) extends Actor {

  private val bus = context.system.lilaBus

  private var onlines = Map[ID, LightUser]()

  def receive = {

    case _ =>
  }

}
