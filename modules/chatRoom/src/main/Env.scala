package lila.chatRoom

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.{ExpireSetMemo, MongoCache}
import scala.concurrent.duration._

final class Env(
                   config: Config,
                   db: lila.db.Env,
                   hub: lila.hub.Env,
                   //getOnlineUserIds: () => Set[String],
                   lightUser: String => Option[lila.common.LightUser],
                   mongoCache: MongoCache.Builder,
                   scheduler: lila.common.Scheduler,
                   system: ActorSystem) {

  private val settings = new {
    val collectionRoomInfo = config getString "collection.roomInfo"
    val collectionRoomMessage = config getString "collection.roomMessage"
//    val PaginatorMaxPerPage = 10 //config getInt "paginator.max_per_page"
    val CachedNbTtl = 10 second  //config duration "cached.nb.ttl"
//    val OnlineTtl = 10 second //config duration "online.ttl"
    val ActorName = config getString "actor.name"
  }
  import settings._

  private[chatRoom] lazy val roomInfoColl = db(collectionRoomInfo)
  private[chatRoom] lazy val roomMessageColl = db(collectionRoomMessage)

  lazy val cached = new Cached(
    nbTtl = CachedNbTtl,
    mongoCache = mongoCache)

  lazy val api = new Api(
    cached = cached,
    actor = hub.actor.relation,
    bus = system.lilaBus)

  private[chatRoom] val actor = system.actorOf(Props(new ChatRoomActor(
    lightUser = lightUser,
    api = api
  )), name = ActorName)

}

object Env {
  lazy val current = "chatRoom" boot new Env(
    config = lila.common.PlayApp loadConfig "chatRoom",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    //getOnlineUserIds = () => lila.user.Env.current.onlineUserIdMemo.keySet,
    lightUser = lila.user.Env.current.lightUser,
    mongoCache = lila.memo.Env.current.mongoCache,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system)
}
