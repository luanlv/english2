package lila.vocab

import akka.actor.ActorSelection
import lila.common.LightUser
import org.joda.time.DateTime

import scala.util.Success
import lila.hub.actorApi.relation.ReloadOnlineFriends
import lila.hub.actorApi.timeline.{Propagate, Follow => FollowUser}
import play.api.libs.json.{JsObject, JsValue, Json}
import spray.json._
final class Api(
                           cached: Cached,
                           actor: ActorSelection,
                           bus: lila.common.Bus) {

  private def counter = lila.counter.Env.current.api

  def test1 = CountersRepo.init("all")

  def newQuestion(data: JsObject) = {
    val newData = Vocab(
      id = counter.getNextId("vocab"),
      typeNum = (data \ "typeNum").as[Int],
      question = (data \ "question").as[String],
      answers = (data \ "answers").as[String],
      key = (data \ "key").as[String]
    )
    VocabRepo.insert(newData)map {
      result =>
        if (result.ok) {
          newData.id
        } else {
          -1
        }
    }
  }

  def getQuestion(id: Int) = {
    VocabRepo.get(id)
  }

}
