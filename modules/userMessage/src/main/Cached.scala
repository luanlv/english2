package lila.userMessage

import scala.concurrent.Future
import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import reactivemongo.bson._

import spray.caching.{ LruCache, Cache }

import lila.common.LightUser
import lila.db.BSON._
import lila.memo.{ ExpireSetMemo, MongoCache }

final class Cached(
                      nbTtl: FiniteDuration,
                      mongoCache: MongoCache.Builder) {

  private def oneWeekAgo = DateTime.now minusWeeks 1

  private val cache: Cache[Int] = LruCache(timeToLive = 1 hour)
  private val cacheVersion: Cache[Map[Int, String]] = LruCache(timeToLive = 5 minute, maxCapacity = 100000)


  def pushVersion(uid:String, v: Int, mid: String) =  {
    cacheVersion.apply(uid + v)(scala.Predef.Map(v -> mid))
  }

  def mapVersion(uid: String, v: Int) = cacheVersion.get(uid + v)

  def chatVersion(mesId: String): Fu[Int] = cache("chatVer:" + mesId) {
     val v = MessageRepo.lastMesVersion(mesId)
     v
  }
  def userChatVersion(userId: String): Fu[Int] = cache("userChatVer:" + userId) {
    MessageRepo.lastUserMesVersion(userId)
  }

  def setNewVersion(id: String, v: Int) = {
    cache.remove(id)
    cache.apply(id)(v)
  }

  def setNewVersion(pre: String, id1: String, v1: Int, id2:String, v2: Int) = {
    cache.remove(pre + id1)
    cache.remove(pre + id2)
    cache.apply(pre + id1)(v1)
    cache.apply(pre + id2)(v2)
  }

  def getNotify(uid: String) = cache("notify:" + uid) {
    NotifyRepo.getNotify(uid)
  }


  def clearCache(pre: String, uid: String) = cache.remove(pre + uid)

}