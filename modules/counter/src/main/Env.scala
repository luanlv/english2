package lila.counter

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.{ExpireSetMemo, MongoCache}
import scala.concurrent.duration._

final class Env(
                 config: Config,
                 db: lila.db.Env,
                 hub: lila.hub.Env,
                 mongoCache: MongoCache.Builder,
                 scheduler: lila.common.Scheduler,
                 system: ActorSystem) {

  private val settings = new {
    val collectionCounters = config getString "collection.counter"
    val CachedNbTtl = 10 second  //config duration "cached.nb.ttl"
    val OnlineTtl = 10 second //config duration "online.ttl"
  }
  import settings._
  
  lazy val cached = new Cached(
    nbTtl = CachedNbTtl,
    mongoCache = mongoCache)

  lazy val api = new Api(
    cached = cached,
    bus = system.lilaBus)


  private[counter] lazy val counterColl = db(collectionCounters)

}

object Env {
  println("counter ====================================================")
  lazy val current = "Counter" boot new Env(
    config = lila.common.PlayApp loadConfig "counter",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system)
}
