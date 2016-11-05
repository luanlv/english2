package lila.chatRoom

import lila.common.LightUser
import org.joda.time.DateTime
import play.api.libs.json._

private[chatRoom] case class RoomMessage(
                                               //mid: String,
                                               roomId: String,
                                               user: LightUser,
                                               chat: String,
                                               time: DateTime)

private[chatRoom] object RoomMessage {
  implicit val lightUser = Json.format[LightUser]
  implicit val formatUserMessage = Json.format[RoomMessage]

  import reactivemongo.bson._

  private[chatRoom] implicit val BSONReader = new BSONDocumentReader[RoomMessage] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }
    def read(doc: BSONDocument): RoomMessage = {
      RoomMessage(
        roomId = ~doc.getAs[String]("roomId"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("user")).head,
        chat = ~doc.getAs[String]("chat"),
        time = doc.getAs[DateTime]("time").head

      )
    }
  }

}