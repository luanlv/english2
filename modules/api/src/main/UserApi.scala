package lila.api

import play.api.libs.json._

import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.common.PimpedJson._
import lila.db.dsl._
import lila.user.{ UserRepo, User, Perfs, Profile }
import makeTimeout.short

private[api] final class UserApi(
    jsonView: lila.user.JsonView,
    relationApi: lila.relation.RelationApi,
    prefApi: lila.pref.PrefApi,
    makeUrl: String => String) {

  def pager(pag: Paginator[User]): JsObject =
    Json.obj("paginator" -> PaginatorJson(pag.mapResults { u =>
      jsonView(u) ++ Json.obj("url" -> makeUrl(s"@/${u.username}"))
    }))
}
