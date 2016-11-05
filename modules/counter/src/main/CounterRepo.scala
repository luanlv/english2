package lila.counter


import scala.concurrent.Future
import scala.concurrent.duration._
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._
import lila.db.dsl._
import spray.caching.{Cache, LruCache}
import lila.common.LightUser
import lila.db.BSON._

import scala.concurrent.ExecutionContext.Implicits.global
import lila.common.LilaCookie
import lila.common.PimpedJson._
import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import reactivemongo.core.commands.Update

object CounterRepo {

  private lazy val coll = Env.current.counterColl


  def init(name: String) = coll.insert($doc("_id" -> name, "seq" -> 0))

  def initAll = coll.insert($doc("_id" -> "all", "seq" -> 0))

  def getNextSequence(counterName: String) = {
    val name = counterName
    val seq = coll.findAndUpdate(
      selector = $doc("_id" -> name),
      update = $inc("seq" -> 1),
      fetchNewObject = true,
      upsert = true)
      .map(_.value.get.getAs[Int]("seq"))
    seq.awaitSeconds(2) match {
      case None => 1
      case Some(int) => int
    }
  }
}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]