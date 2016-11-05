package lila.question

import akka.actor._
import com.typesafe.config.Config

import lila.common.PimpedConfig._
import lila.memo.{ExpireSetMemo, MongoCache}
import scala.concurrent.duration._

final class Env(
                 config: Config,
                 db: lila.db.Env,
                 hub: lila.hub.Env,
                 lightUser: String => Option[lila.common.LightUser],
                 mongoCache: MongoCache.Builder,
                 scheduler: lila.common.Scheduler,
                 system: ActorSystem) {

  private val settings = new {
    val collectionQuestionColl = config getString "collection.question"
    val collectionAnswerColl = config getString "collection.answer"
    val collectionCommentQAColl = config getString "collection.commentQA"
    //    val collectionNotify = config getString "collection.notify"
    val PaginatorMaxPerPage = 10 //config getInt "paginator.max_per_page"
    val CachedNbTtl = 10 second  //config duration "cached.nb.ttl"
    val OnlineTtl = 10 second //config duration "online.ttl"
    val ActorName = config getString "actor.name"
  }
  import settings._


  lazy val cached = new Cached(
    nbTtl = CachedNbTtl,
    mongoCache = mongoCache)

  lazy val questionApi = new QuestionApi(
    cached = cached,
    actor = hub.actor.activity
  )

  lazy val answerApi = new AnswerApi(
    cached = cached,
    actor = hub.actor.activity
  )

  lazy val commentApi = new CommentApi(
    cached = cached,
    actor = hub.actor.activity
  )

  private[question] val actor = system.actorOf(Props(new QuestionActor(
    questionApi = questionApi,
    answerApi = answerApi,
    commentApi = commentApi
  )), name = ActorName)

  private[question] lazy val questionColl = db(collectionQuestionColl)
  private[question] lazy val answerColl = db(collectionAnswerColl)
  private[question] lazy val commentQAColl = db(collectionCommentQAColl)

}

object Env {
  lazy val current = "question" boot new Env(
    config = lila.common.PlayApp loadConfig "question",
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    lightUser = lila.user.Env.current.lightUser,
    mongoCache = lila.memo.Env.current.mongoCache,
    scheduler = lila.common.PlayApp.scheduler,
    system = lila.common.PlayApp.system)
}
