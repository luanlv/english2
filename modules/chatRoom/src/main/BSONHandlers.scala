package lila.chatRoom

import lila.common.LightUser
import lila.user.LightUserApi
import lila.db.BSON
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {
  implicit val messageBSONHandler = new BSON[RoomMessage] {
    def reads(r: BSON.Reader) = {
      RoomMessage(
        roomId = r str "roomId",
        user = lila.user.Env.current.lightUserApi.get(r str "user").head,
        chat = r str "chat",
        time = r date "time")
    }
    def writes(w: BSON.Writer, o: RoomMessage) = {
      BSONDocument(
        //"_id" -> o._id,
        "roomId" -> o.roomId,
        "user" -> o.user.id,
        "chat" -> o.chat,
        "time" -> w.date(o.time))
    }
  }

}