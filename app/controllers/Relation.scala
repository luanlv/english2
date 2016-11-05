package controllers

import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html

import lila.api.Context
import lila.app._
import lila.relation.Related
import lila.user.{ User => UserModel, UserRepo }
import views._

object Relation extends LilaController {

  private def env = Env.relation

  private def renderActions(userId: String, mini: Boolean)(implicit ctx: Context) =
      (ctx.userId ?? { env.api.fetchRelation(_, userId) }) zip
      (ctx.userId ?? { env.api.fetchRelation(userId, _) }) zip
      (ctx.isAuth ?? { Env.pref.api followable userId }) zip
      (ctx.userId ?? { env.api.fetchBlocks(userId, _) }) flatMap {
      case (((relation, relation2), followable), blocked) => {
        negotiate(
          html = fuccess(Ok(mini.fold(
            html.relation.mini(userId, blocked = blocked, followable = followable, relation = relation, relation2 = relation2 ),
            html.relation.actions2(userId, relation = relation, relation2 = relation2, followable = followable, blocked = blocked)
          ))),
          api = _ => fuccess(Ok(Json.obj(
            "followable" -> followable,
            "following" -> relation.exists(true ==),
            "blocking" -> relation.exists(false ==)
          )))
        )
      }
    }


  def follow(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.follow(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }


  def unfollow(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.unfollow(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def reject(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.unfollow(userId, me.id).nevermind
  }

  def unfriend(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.unfollow(me.id, userId).nevermind >> env.api.unfollow(userId, me.id).nevermind >> renderActions(userId, getBool("mini"))
  }

  def block(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.block(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  def unblock(userId: String) = Auth { implicit ctx =>
    me =>
      env.api.unblock(me.id, userId).nevermind >> renderActions(userId, getBool("mini"))
  }

  //  def following(username: String) = Open { implicit ctx =>
  //    OptionFuOk(UserRepo named username) { user =>
  //      env.api.following(user.id) flatMap followship flatMap { rels =>
  //        env.api nbFollowers user.id map { followers =>
  //          html.relation.following(user, rels, followers)
  //        }
  //      }
  //    }
  //  }

  //  def followers(username: String) = Open { implicit ctx =>
  //    OptionFuOk(UserRepo named username) { user =>
  //      env.api.followers(user.id) flatMap followship flatMap { rels =>
  //        env.api nbFollowing user.id map { following =>
  //          html.relation.followers(user, rels, following)
  //        }
  //      }
  //    }
  //  }

  //  def blocks = Auth { implicit ctx =>
  //    me =>
  //      env.api.blocking(me.id) flatMap followship map { rels =>
  //        html.relation.blocks(me, rels)
  //      }
  //  }

  private def followship(userIds: Set[String])(implicit ctx: Context): Fu[List[Related]] =
    UserRepo byIds userIds flatMap { users =>
      (ctx.isAuth ?? { Env.pref.api.followableIds(users map (_.id)) }) flatMap { followables =>
        users.map { u =>
          ctx.userId ?? { env.api.fetchRelation(_, u.id) } map { rel =>
            lila.relation.Related(u, Some(0), followables(u.id), rel)
          }
        }.sequenceFu
      }
    }
}
