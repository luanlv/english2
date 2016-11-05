package lila.userMessage

import lila.common.LightUser
import lila.userMessage
import org.joda.time.DateTime
import play.api.libs.json._

private[userMessage] case class Notify(
                                      id: String,
                                      m: List[NotifyMessage],
                                      n: Int,
                                      ur: List[String]
                                     )

private[userMessage] object Notify {

  implicit val formatLightUser = Json.format[LightUser]
  implicit val formatNotify = Json.format[Notify]
  import reactivemongo.bson._

  private[userMessage] implicit val BSONReader = new BSONDocumentReader[Notify] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }

    def read(doc: BSONDocument): Notify = {
      Notify(
        id = ~doc.getAs[String]("_id"),
        m = ~doc.getAs[List[NotifyMessage]]("m"),
        n = ~doc.getAs[Int]("n"),
        ur = ~doc.getAs[List[String]]("ur")
      )
    }
  }
}


private[userMessage] case class NotifyMessage(
                                               user: LightUser,
                                               n: Int,
                                               d: DateTime,
                                               lm: UserMessage)

private[userMessage] object NotifyMessage {

  implicit val formatLightUser = Json.format[LightUser]
  implicit val formatUserMessage = Json.format[UserMessage]
  implicit val formatNotifyMessage = Json.format[NotifyMessage]

  import reactivemongo.bson._

  private[userMessage] implicit val BSONReader = new BSONDocumentReader[NotifyMessage] {

    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }

    def read(doc: BSONDocument): NotifyMessage = {
      NotifyMessage(
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("uid")).head,
        n = ~doc.getAs[Int]("n"),
        d = doc.getAs[DateTime]("d").head,
        lm = doc.getAs[UserMessage]("lm").get
      )
    }
  }
}
