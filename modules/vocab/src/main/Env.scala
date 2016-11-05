package lila.vocab

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
    val collectionVocabCall = config getString "collection.vocab"
    val collectionCounters = config getString "collection.counters"
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
    actor = hub.actor.vocab,
    bus = system.lilaBus)

  private[vocab] val actor = system.actorOf(Props(new VocabActor(
    getOnlineUserIds = getOnlineUserIds,
    lightUser = lightUser,
    api = api
  )), name = ActorName)



  private[vocab] lazy val vocabColl = db(collectionVocabCall)
  private[vocab] lazy val countersColl = db(collectionCounters)

}

object Env {
  lazy val current = "Vocab" boot new Env(
    config = lila.common.PlayApp loadConfig "vocab",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    getOnlineUserIds = () => lila.user.Env.current.onlineUserIdMemo.keySet,
    lightUser = lila.user.Env.current.lightUser,
    mongoCache = lila.memo.Env.current.mongoCache,

    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system)
}
