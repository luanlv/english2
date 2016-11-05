package lila.app

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

final class Env(
    config: Config,
    val scheduler: lila.common.Scheduler,
    system: ActorSystem,
    appPath: String) {

  val CliUsername = config getString "cli.username"

  private val RendererName = config getString "app.renderer.name"

  lazy val bus = lila.common.Bus(system)

  system.actorOf(Props(new actor.Renderer), name = RendererName)

  lila.log.boot.info("Preloading modules")
  lila.common.Chronometer.syncEffect(List(
    Env.socket,
    Env.relation,
    Env.counter,
    Env.game,
    Env.site,
    Env.pref,
    Env.notification,
    Env.activity,
    Env.question,
    Env.chatRoom,
    Env.userMessage,
    Env.vocab
  )) { lap =>
    lila.log("boot").info(s"${lap.millis}ms Preloading complete")
  }

}

object Env {

  lazy val current = "app" boot new Env(
    config = lila.common.PlayApp.loadConfig,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system,
    appPath = lila.common.PlayApp withApp (_.path.getCanonicalPath))


  def db = lila.db.Env.current
  def user = lila.user.Env.current
  def security = lila.security.Env.current
  def hub = lila.hub.Env.current
  def socket = lila.socket.Env.current
  def i18n = lila.i18n.Env.current
  def relation = lila.relation.Env.current
  def game = lila.game.Env.current
  def site = lila.site.Env.current
  def pref = lila.pref.Env.current
  def notification = lila.notification.Env.current
  def activity = lila.activity.Env.current
  def question = lila.question.Env.current
  def chatRoom = lila.chatRoom.Env.current
  def userMessage = lila.userMessage.Env.current

  def counter = lila.counter.Env.current

  def vocab = lila.vocab.Env.current
  def api = lila.api.Env.current


}
