package lila.question

import lila.common.LightUser
import org.joda.time.DateTime
import play.api.libs.json._

private[question] case class Vote(
                                 userId: String,
                                 vote: Int
                                 )

private[question] object Vote {
  implicit val formatVote = Json.format[Vote]

  import reactivemongo.bson._

  private[question] implicit val BSONReaderVote = new BSONDocumentReader[Vote] {

    def read(doc: BSONDocument): Vote = {
      Vote(
        userId = ~doc.getAs[String]("userId"),
        vote = ~doc.getAs[Int]("vote")
      )
    }
  }

}

private [question] case class DayViews(
                                      date: DateTime = DateTime.now(),
                                      views: Int = 0
                                      )

private[question] object DayViews {
  implicit val formatDayViews = Json.format[DayViews]

  import reactivemongo.bson._

  private[question] implicit val BSONReaderVote = new BSONDocumentReader[DayViews] {

    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }

    def read(doc: BSONDocument): DayViews = {
      DayViews(
        date = doc.getAs[DateTime]("date").head,
        views = ~doc.getAs[Int]("views")
      )
    }
  }
}

private[question] case class Comment(
                                           id: String,
                                           parentId: String,
                                           comment: String,
                                           user: LightUser,
                                           time: DateTime = DateTime.now()
                                         )


private[question] object Comment {
  implicit val lightUser = Json.format[LightUser]
  implicit val formatChildComment = Json.format[Comment]

  import reactivemongo.bson._

  private[question] implicit val BSONReaderChildComment = new BSONDocumentReader[Comment] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }
    def read(doc: BSONDocument): Comment = {
      Comment(
        id = ~doc.getAs[String]("_id"),
        parentId = ~doc.getAs[String]("parentId"),
        comment = ~doc.getAs[String]("comment"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("userId")).head,
        time = doc.getAs[DateTime]("time").head
      )
    }
  }
}


private[question] case class Question(
                                   id: String,
                                   question: String,
                                   description: String,
                                   user: LightUser,
                                   published: DateTime = DateTime.now(),
                                   views: Int = 0,
                                   dayViews: Option[List[DayViews]] = Option(List(DayViews())),
                                   voteCount: Int = 0,
                                   votes: Option[List[Vote]] = Option(List()),
                                   shareCount: Int = 0,
                                   shares: List[String] = List(),
                                   commentCount: Int = 0,
                                   comment: Option[List[Comment]] = Option(List()),
                                   answerCount: Int = 0
                                 )

private[question] object Question {
  implicit val lightUser = Json.format[LightUser]
  implicit val formatComment = Json.format[Comment]
  implicit val formatQuestion = Json.format[Question]

  import reactivemongo.bson._

  private[question] implicit val BSONReaderQuestion = new BSONDocumentReader[Question] {

    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }
//    implicit val formatDayViews = Json.format[DayViews]
    def read(doc: BSONDocument): Question = {
      Question(
        id = ~doc.getAs[String]("_id"),
        question = ~doc.getAs[String]("question"),
        description = ~doc.getAs[String]("description"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("userId")).head,
        published = doc.getAs[DateTime]("published").head,
        views = ~doc.getAs[Int]("views"),
        dayViews = doc.getAs[List[DayViews]]("dayViews"),
        voteCount = ~doc.getAs[Int]("voteCount"),
        votes = doc.getAs[List[Vote]]("votes"),
        shareCount = ~doc.getAs[Int]("shareCount"),
        shares = ~doc.getAs[List[String]]("shares"),
        commentCount = ~doc.getAs[Int]("commentCount"),
        comment = doc.getAs[List[Comment]]("comment"),
        answerCount = ~doc.getAs[Int]("answerCount")
      )
    }
  }

}

private[question] case class Answer(
                                       id: String,
                                       qId: String,
                                       answer: String,
                                       user: LightUser,
                                       published: DateTime = DateTime.now(),
                                       voteCount: Int = 0,
                                       votes: Option[List[Vote]] = Option(List()),
                                       commentCount: Int = 0,
                                       comment: Option[List[Comment]] = Option(List())
                                     )

private[question] object Answer {

  implicit val lightUser = Json.format[LightUser]
  implicit val formatComment = Json.format[Comment]
  implicit val formatAnswer = Json.format[Answer]

  import reactivemongo.bson._

  private[question] implicit val BSONReaderAnser = new BSONDocumentReader[Answer] {

    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }

    def read(doc: BSONDocument): Answer = {
      Answer(
        id = ~doc.getAs[String]("_id"),
        qId = ~doc.getAs[String]("qId"),
        answer = ~doc.getAs[String]("answer"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("userId")).head,
        published = doc.getAs[DateTime]("published").head,
        voteCount = ~doc.getAs[Int]("voteCount"),
        votes = doc.getAs[List[Vote]]("votes"),
        commentCount = ~doc.getAs[Int]("commentCount"),
        comment = doc.getAs[List[Comment]]("comment")
      )
    }
  }

}