package lila.userMessage

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.{ExpireSetMemo, MongoCache}
import scala.concurrent.duration._

final class Env(
                   config: Config,
                   db: lila.db.Env,
                   hub: lila.hub.Env,
                   getOnlineUserIds: () => Set[String],
                   lightUser: String => Option[lila.common.LightUser],
                   mongoCache: MongoCache.Builder,
                   scheduler: lila.common.Scheduler,
                   system: ActorSystem) {

  private val settings = new {
    val collectionUserMessage = config getString "collection.userMessage"
    val collectionNotify = config getString "collection.notify"
    val PaginatorMaxPerPage = 10 //config getInt "paginator.max_per_page"
    val CachedNbTtl = 10 second  //config duration "cached.nb.ttl"
    val OnlineTtl = 10 second //config duration "online.ttl"
    val ActorName = config getString "actor.name"
  }
  import settings._
  lazy val cached = new Cached(
    nbTtl = CachedNbTtl,
    mongoCache = mongoCache)

  lazy val api = new Api(
    cached = cached,
    actor = hub.actor.relation,
    bus = system.lilaBus)

  private[userMessage] val actor = system.actorOf(Props(new UserMessageActor(
    getOnlineUserIds = getOnlineUserIds,
    lightUser = lightUser,
    api = api
  )), name = ActorName)



  //lazy val messageRepo = new MessageRepo(repo = userMessage)
  //lazy val notifyRepo = new NotifyRepo(repo = notifyMessage)

//  scheduler.once(10 seconds) {
//    scheduler.message(1.02 second) {
//      actor -> lila.userMessage.actorApi.NotifyMovement
//    }
//  }

  private[userMessage] lazy val userMessageColl = db(collectionUserMessage)
  private[userMessage] lazy val notifyColl = db(collectionNotify)

}

object Env {
  lazy val current = "userMessage" boot new Env(
    config = lila.common.PlayApp loadConfig "userMessage",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    getOnlineUserIds = () => lila.user.Env.current.onlineUserIdMemo.keySet,
    lightUser = lila.user.Env.current.lightUser,
    mongoCache = lila.memo.Env.current.mongoCache,

    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system)
}
