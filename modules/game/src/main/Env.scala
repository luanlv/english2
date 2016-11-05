package lila.game

import akka.actor._
import akka.pattern.pipe
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.game.Captcher.SayHello
import lila.game.actorApi

final class Env(
    config: Config,
    db: lila.db.Env,
    mongoCache: lila.memo.MongoCache.Builder,
    system: ActorSystem,
    hub: lila.hub.Env,
    getLightUser: String => Option[lila.common.LightUser],
    appPath: String,
    isProd: Boolean,
    scheduler: lila.common.Scheduler) {

  private val settings = new {
    val CachedNbTtl = config duration "cached.nb.ttl"
    val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
    val CaptcherName = config getString "captcher.name"
    val CaptcherDuration = config duration "captcher.duration"
    val CollectionGame = config getString "collection.game"
    val CollectionCrosstable = config getString "collection.crosstable"
    val JsPathRaw = config getString "js_path.raw"
    val JsPathCompiled = config getString "js_path.compiled"
    val ActorName = config getString "actor.name"
    val UciMemoTtl = config duration "uci_memo.ttl"
    val netBaseUrl = config getString "net.base_url"
    val PdfExecPath = config getString "pdf.exec_path"
    val PngExecPath = config getString "png.exec_path"
  }
  import settings._

//  val MandatorySecondsToMove = config getInt "mandatory.seconds_to_move"

  private[game] lazy val gameColl = db(CollectionGame)


  lazy val gameJs = new GameJs(path = jsPath, useCache = isProd)

  // load captcher actor
  val captcher = {
    system.actorOf(Props(new Captcher), name = CaptcherName)
  }

  import akka.pattern.ask

//  def testCaptcher ={
//    import makeTimeout.large
//    (captcher ? SayHello("luan")).mapTo[String]
//  }


  private def jsPath =
    "%s/%s".format(appPath, isProd.fold(JsPathCompiled, JsPathRaw))
}

object Env {
  lazy val current = "game" boot new Env(
    config = lila.common.PlayApp loadConfig "game",
    db = lila.db.Env.current,
    mongoCache = lila.memo.Env.current.mongoCache,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    getLightUser = lila.user.Env.current.lightUser,
    appPath = play.api.Play.current.path.getCanonicalPath,
    isProd = lila.common.PlayApp.isProd,
    scheduler = lila.common.PlayApp.scheduler)
}
