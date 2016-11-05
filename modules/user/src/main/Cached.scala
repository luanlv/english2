package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._

import lila.common.LightUser
import lila.db.BSON
import lila.db.dsl._
import lila.memo.{ ExpireSetMemo, MongoCache }

final class Cached(
    userColl: Coll,
    nbTtl: FiniteDuration,
    onlineUserIdMemo: ExpireSetMemo,
    mongoCache: MongoCache.Builder) {

  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1

  private val countCache = mongoCache.single[Int](
    prefix = "user:nb",
    f = userColl.count(UserRepo.enabledSelect.some),
    timeToLive = nbTtl)

  private implicit val LightUserBSONHandler = Macros.handler[LightUser]



  val top50Online = lila.memo.AsyncCache.single[List[User]](
    f = UserRepo.byIdsSortRating(onlineUserIdMemo.keys, 50),
    timeToLive = 10 seconds)

  val topToints = mongoCache.single[List[User]](
    prefix = "user:top:toints",
    f = UserRepo allSortToints 10,
    timeToLive = 5 minutes)

}
