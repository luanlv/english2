package lila.api

import akka.actor._
import com.typesafe.config.Config
import lila.common.PimpedConfig._
import scala.collection.JavaConversions._
import scala.concurrent.duration._

final class Env(
                 config: Config,
                 db: lila.db.Env,
                 renderer: ActorSelection,
                 system: ActorSystem,
                 scheduler: lila.common.Scheduler,
                 relationApi: lila.relation.RelationApi,
                 userEnv: lila.user.Env,
                 val isProd: Boolean) {


  val CliUsername = config getString "cli.username"

  val apiToken = config getString "api.token"

  object Net {
    val Domain = config getString "net.domain"
    val Protocol = config getString "net.protocol"
    val BaseUrl = config getString "net.base_url"
    val Port = config getInt "http.port"
    val AssetDomain = config getString "net.asset.domain"
    val AssetVersion = config getInt "net.asset.version"
    val Email = config getString "net.email"
  }
  println(Net)
  val PrismicApiUrl = config getString "prismic.api_url"
  val EditorAnimationDuration = config duration "editor.animation.duration"
  val ExplorerEndpoint = config getString "explorer.endpoint"
  val TablebaseEndpoint = config getString "explorer.tablebase.endpoint"

  object assetVersion {
    import reactivemongo.bson._
    import lila.db.dsl._
    private val coll = db("flag")
    private val cache = lila.memo.MixedCache.single[Int](
      f = coll.primitiveOne[BSONNumberLike]($id("asset"), "version").map {
        _.fold(Net.AssetVersion)(_.toInt max Net.AssetVersion)
      },
      timeToLive = 10.seconds,
      default = Net.AssetVersion,
      logger = lila.log("assetVersion"))
    def get = cache get true
  }

  object Accessibility {
    val blindCookieName = config getString "accessibility.blind.cookie.name"
    val blindCookieMaxAge = config getInt "accessibility.blind.cookie.max_age"
    private val blindCookieSalt = config getString "accessibility.blind.cookie.salt"
    def hash(implicit ctx: lila.user.UserContext) = {
      import com.roundeights.hasher.Implicits._
      (ctx.userId | "anon").salt(blindCookieSalt).md5.hex
    }
  }



  private def makeUrl(path: String): String = s"${Net.BaseUrl}/$path"

  lazy val cli = new Cli(system.lilaBus, renderer)

  KamonPusher.start(system) {
    new KamonPusher(countUsers = () => userEnv.onlineUserIdMemo.count)
  }


}

object Env {

  lazy val current = "api" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    db = lila.db.Env.current,
    renderer = lila.hub.Env.current.actor.renderer,
    userEnv = lila.user.Env.current,
    relationApi = lila.relation.Env.current.api,
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler,
    isProd = lila.common.PlayApp.isProd)
}