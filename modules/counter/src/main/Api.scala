package lila.counter

import akka.actor.ActorSelection
import lila.common.LightUser
import org.joda.time.DateTime

import scala.util.Success
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{Propagate, Follow => FollowUser}
import play.api.libs.json.{JsObject, JsValue}

final class Api(
                 cached: Cached,
                 bus: lila.common.Bus) {

  def test1 = CounterRepo.init("all")

  def getNextId(ident: String) = CounterRepo.getNextSequence(ident)


}
