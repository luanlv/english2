package lila.relation

import akka.actor.{Actor, ActorSelection}
import akka.pattern.{ask, pipe}
import lila.memo.ExpireSetMemo
import play.api.libs.json.Json

import scala.concurrent.duration._
import actorApi._
import lila.common.LightUser
import lila.hub.actorApi.relation._
import lila.hub.actorApi.userMessage.PingVersion
import lila.hub.actorApi.{SendTo, SendTos}
import makeTimeout.short

private[relation] final class RelationActor(
    getOnlineUserIds: () => Set[String],
    lightUser: String => Option[LightUser],
    api: RelationApi) extends Actor {

  private val bus = context.system.lilaBus

  private var onlines = Map[ID, LightUser]()

  private val onlinePlayings = new ExpireSetMemo(1 hour)

  override def preStart(): Unit = {
    context.system.lilaBus.subscribe(self, 'startGame)
    context.system.lilaBus.subscribe(self, 'finishGame)
  }

  override def postStop() : Unit = {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
  }

  def receive = {

    case PingVersion(userId) => {
      sender ! api.countFriendRequests(userId).await
    }

    case GetFriendRequest(userId) => {
      sender ! api.fetchFriendRequests(userId).map(setId => setId.map(id => lightUser(id).get))
    }

    case GetOnlineUser(userId) => sender ! onlineFriends2(userId).await

    case GetFriends(userId) => {
      val friends = api.fetchFriends(userId).await
      sender ! friends.map(id => lightUser(id).get).toList
    }

    case GetOnlineFriends(userId) => onlineFriends2(userId) pipeTo sender

    // triggers following reloading for this user id
    case ReloadOnlineFriends(userId) => onlineFriends2(userId) foreach {
      case onlineFriends =>
        bus.publish(SendTo(userId, JsonView.writeOnlineFriends(onlineFriends)), 'users)
    }

    case NotifyMovement =>
      val prevIds = onlineIds
      val curIds = getOnlineUserIds()
      val leaveIds = (prevIds diff curIds).toList
      val enterIds = (curIds diff prevIds).toList
      val leaves = leaveIds.flatMap(i => lightUser(i))
      val enters = enterIds.flatMap(i => lightUser(i))
      onlines = onlines -- leaveIds ++ enters.map(e => e.id -> e)

      val friendsEntering = enters.map(makeFriendEntering)
//      notifyFollowersFriendEnters(friendsEntering)
//      notifyFollowersFriendLeaves(leaves)
      notifyFollowers(enters, "following_enters")
      notifyFollowers(leaves, "following_leaves")
  }

  private def makeFriendEntering(enters: LightUser) = {
    FriendEntering(enters, onlinePlayings.get(enters.id))
  }

  private def onlineIds: Set[ID] = onlines.keySet

  private def onlineFriends(userId: String): Fu[OnlineFriends] =
    api fetchFollowing userId map { ids =>
      val friends = ids.flatMap(onlines.get).toList
      val friendsPlaying = filterFriendsPlaying(friends)
      OnlineFriends(friends, friendsPlaying)
    }

  private def onlineFriends2(userId: String): Fu[List[LightUser]] =
    api fetchFriends userId map { ids =>
      ids.flatMap(onlines.get).toList
    }


  private def filterFriendsPlaying(friends: List[LightUser]): Set[String] = {
    friends.filter(p => onlinePlayings.get(p.id)).map(_.id).toSet
  }

  private def notifyFollowersFriendEnters(friendsEntering: List[FriendEntering]) =
    friendsEntering foreach { entering =>
      api fetchFollowers entering.user.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, JsonView.writeFriendEntering(entering)), 'users)
      }
    }

  private def notifyFollowersFriendLeaves(friendsLeaving: List[LightUser]) =
    friendsLeaving foreach { leaving =>
      api fetchFollowers leaving.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, "following_leaves", leaving.titleName), 'users)
      }
    }

  private def notifyFollowers(users: List[LightUser], message: String) {
    users foreach { user =>
      api fetchFriends  user.id map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, user), 'users)
      }
    }
  }


  private def notifyFollowersGameStateChanged(userIds: Traversable[String], message: String) =
    userIds foreach { userId =>
      api fetchFollowers userId map (_ filter onlines.contains) foreach { ids =>
        if (ids.nonEmpty) bus.publish(SendTos(ids.toSet, message, userId), 'users)
      }
    }
}
