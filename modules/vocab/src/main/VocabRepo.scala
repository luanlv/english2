package lila.vocab


import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
//import play.api.libs.json._
import reactivemongo.bson._
import lila.db.dsl._

import spray.caching.{ LruCache, Cache }

import lila.common.LightUser
import lila.db.BSON._
import scala.concurrent.ExecutionContext.Implicits.global
import lila.common.LilaCookie
import lila.common.PimpedJson._
import lila.db.dsl._
import lila.db.BSON.BSONJodaDateTimeHandler
import BSONHandlers._
object VocabRepo {

  private lazy val coll = Env.current.vocabColl



  def insert(data: Vocab) = {
    coll.insert(data)
  }

  def get(id: Int) = {
    coll.uno[Vocab]($doc("_id" -> id))
  }

}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]