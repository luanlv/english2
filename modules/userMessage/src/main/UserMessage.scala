package lila.userMessage

import lila.common.LightUser
import org.joda.time.DateTime
import play.api.libs.json._


private[userMessage] case class UserMessage(
                                               //_id: String,
                                               //mid: String,
                                               mv: Int,
                                               f: LightUser,
                                               t: LightUser,
                                               mes: String,
                                               time: DateTime)

private[userMessage] object UserMessage {
  implicit val lightUser = Json.format[LightUser]
  implicit val formatUserMessage = Json.format[UserMessage]

  import reactivemongo.bson._

  private[userMessage] implicit val BSONReader = new BSONDocumentReader[UserMessage] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }
    def read(doc: BSONDocument): UserMessage = {
      UserMessage(
        mv = ~doc.getAs[Int]("mv"),
        f = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("f")).head,
        t = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("t")).head,
        mes = ~doc.getAs[String]("mes"),
        time = doc.getAs[DateTime]("time").head
      )
    }
  }

}