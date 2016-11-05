package lila.vocab

import akka.actor.ActorSelection
import lila.common.LightUser
import org.joda.time.DateTime

import scala.util.Success
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{Propagate, Follow => FollowUser}
import play.api.libs.json.{JsObject, JsValue}
import spray.json._
final class Api(
                           cached: Cached,
                           actor: ActorSelection,
                           bus: lila.common.Bus) {

  def test1 = CountersRepo.init("all")

  def test2(json: JsObject) = {
    VocabRepo.demo(json.toString().parseJson.asJsObject)
  }

}
