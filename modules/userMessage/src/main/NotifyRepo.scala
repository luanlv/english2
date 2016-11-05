package lila.userMessage


import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json._
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

object NotifyRepo {

  private lazy val coll = Env.current.notifyColl

  def notifyMessage(uid: String, chatId: String, mesId: String, mv: Int, mes: String, time: DateTime) = {
    val bs = BSONDocument("_id" -> (mesId + "_" + mv), "mid" -> mesId, "mv" -> mv,  "f" -> chatId, "t" -> uid, "mes" -> mes, "time" -> time)
    if(mv == 1){
      coll.update(
        BSONDocument("_id" -> uid),
        BSONDocument(
          "$push" -> BSONDocument("m" -> BSONDocument("uid" -> chatId, "n" -> 1, "d" -> time, "lm" -> bs)),
          "$inc" -> BSONDocument("n" -> 1),
          "$push" -> BSONDocument("ur" -> chatId)
        ),
        upsert = true
      )
      coll.update(
        BSONDocument("_id" -> chatId),
        BSONDocument(
          "$push" -> BSONDocument("m" -> BSONDocument("uid" -> uid, "n" -> 0, "d" -> time, "lm" -> bs)),
          "$inc" -> BSONDocument("n" -> 0)
        ),
        upsert = true
      )
      true
    } else {
      val data = coll.find(BSONDocument("_id" -> uid, "m.uid" -> chatId), BSONDocument("m.$" -> 1, "n" -> 1, "ur" -> 1))
        .cursor[Notify]()
        .gather[List]().map {
        list => list match {
          case List() => (-1, List())
          case neList => (neList.head.m.head.n , neList.head.ur )
        }
      }

      coll.update(
        BSONDocument("_id" -> chatId, "m" -> BSONDocument("$elemMatch" -> BSONDocument("uid" -> uid))),
        BSONDocument(
          "$set" -> BSONDocument("m.$.d" -> time),
          "$set" -> BSONDocument("m.$.lm" -> bs)
          //"$push" -> BSONDocument("ur" -> chatId)
        )
      )

      val update = data.map{num =>
        if(num._1 == 0) {
          coll.update(
            BSONDocument("_id" -> uid, "m" -> BSONDocument("$elemMatch" -> BSONDocument("uid" -> chatId))),
            BSONDocument(
              "$inc" -> BSONDocument("m.$.n" -> 1),
              "$set" -> BSONDocument("m.$.d" -> time),
              "$inc" -> BSONDocument("n" -> 1),
              "$set" -> BSONDocument("m.$.lm" -> bs),
              "$push" -> BSONDocument("ur" -> chatId)
            )
          )
          true
        } else if(num._1 > 0 && num._2.contains(chatId)) {
          coll.update(
            BSONDocument("_id" -> uid, "m" -> BSONDocument("$elemMatch" -> BSONDocument("uid" -> chatId))),
            BSONDocument(
              "$inc" -> BSONDocument("m.$.n" -> 1),
              "$set" -> BSONDocument("m.$.d" -> time),
              "$set" -> BSONDocument("m.$.lm" -> bs)
            )
          )
          false
        } else {
          coll.update(
            BSONDocument("_id" -> uid, "m" -> BSONDocument("$elemMatch" -> BSONDocument("uid" -> chatId))),
            BSONDocument(
              "$inc" -> BSONDocument("m.$.n" -> 1),
              "$set" -> BSONDocument("m.$.d" -> time),
              "$inc" -> BSONDocument("n" -> 1),
              "$set" -> BSONDocument("m.$.lm" -> bs),
              "$push" -> BSONDocument("ur" -> chatId)
            )
          )
          true
        }
      }
      update.await
    }
  }

  def getNotify(uid: String) = {
    val data = coll.find($doc("_id" -> uid))
      .sort($sort desc "mv")
      .cursor[Notify]()
      .gather[List]().map {
      list => list match {
        case List() => {
          0
        }
        case neList => {
          neList.head.n
        }
      }
    }
    data
  }

  def getNotifyMessage(userId: String) = {
    import coll.BatchCommands.AggregationFramework, AggregationFramework.{ AddToSet, Group, Match, Project, Push, Unwind, Sort, Ascending, Descending, Limit}
    val x = coll.aggregate(
      Match(BSONDocument("_id" -> userId)), List(
      Unwind("m"),
      //Match(BSONDocument("lid.id" -> userId)),
      Sort(Descending("m.d")),
      Limit(5),
      Project(BSONDocument("_id" -> 0, "m" -> 1))
    )).map(_.firstBatch.map(x => x.getAs[BSONDocument]("m").head).map(_.as[NotifyMessage]))
    x.map ( x => {
      x
    })
  }

  def resetNotify(userId: String) = {
    coll.update(
      BSONDocument("_id" -> userId),
      BSONDocument(
        "$set" -> BSONDocument("n" -> 0),
        "$set" -> BSONDocument("ur" -> BSONArray())
      )
    )
  }

  def markRead(userId: String, toId: String, mv: Int) = {
    coll.find(BSONDocument("_id" -> userId, "m.uid" -> toId), BSONDocument("m.$" -> 1, "ur" -> 1, "n" ->1))
      .cursor[Notify]()
      .gather[List]().map {
      list => list match {
        case List() => (0, 0, List())
        case neList => (neList.head.m.head, neList.head.m.head.lm.mv , neList.head.ur)
      }
    }.map{
      num => {
        val unread = num._2 - mv
        if(num._1 != 0 && unread == 0){
          if(num._3.contains(toId)){
            coll.update(
              BSONDocument("_id" -> userId, "m" -> BSONDocument("$elemMatch" -> BSONDocument("uid" -> toId))),
              BSONDocument(
                "$set" -> BSONDocument("m.$.n" -> unread),
                "$inc" -> BSONDocument("n" -> -1),
                "$pull" -> BSONDocument("ur" -> toId)
              )
            )
            true
          } else {
            coll.update(
              BSONDocument("_id" -> userId, "m" -> BSONDocument("$elemMatch" -> BSONDocument("uid" -> toId))),
              BSONDocument(
                "$set" -> BSONDocument("m.$.n" -> unread)
              )
            )
            false
          }
        } else {
          false
        }
      }
    }
  }
}


//val bson = BSONFormats.toBSON(o).get.asInstanceOf[BSONDocument]