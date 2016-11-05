package lila.activity

import lila.common.LightUser
import org.joda.time.DateTime
import play.api.libs.json._

private[activity] case class Post(
                                        id: String,
                                        content: String,
                                        user: LightUser,
                                        published: DateTime = DateTime.now(),
                                        likeCount: Int = 0,
                                        likes: Option[List[String]] = Option(List()),
                                        shareCount: Int = 0,
                                        shares: Option[List[String]] = Option(List()),
                                        commentCount: Int = 0
                                     )

private[activity] object Post {
  implicit val lightUser = Json.format[LightUser]
  implicit val formatPost = Json.format[Post]

  import reactivemongo.bson._

  private[activity] implicit val BSONReaderPost = new BSONDocumentReader[Post] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }
    def read(doc: BSONDocument): Post = {
      Post(
        id = ~doc.getAs[String]("_id"),
        content = ~doc.getAs[String]("content"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("userId")).head,
        published = doc.getAs[DateTime]("published").head,
        likeCount = ~doc.getAs[Int]("likeCount"),
        likes = doc.getAs[List[String]]("likes"),
        shareCount = ~doc.getAs[Int]("shareCount"),
        shares = doc.getAs[List[String]]("shares"),
        commentCount = ~doc.getAs[Int]("commentCount")
      )
    }
  }

}



private[activity] case class Comment(
                                   id: String,
                                   parentPost: String,
                                   comment: String,
                                   user: LightUser,
                                   time: DateTime,
                                   likeCount: Int,
                                   likes: Option[List[String]],
                                   childCount: Int = 0,
                                   children: List[ChildComment]
                                 )

private[activity] case class ChildComment(
                                           id: String,
                                           parentId: String,
                                           comment: String,
                                           user: LightUser,
                                           time: DateTime,
                                           likeCount: Int,
                                           likes: Option[List[String]]
                                         )


private[activity] object ChildComment {
  implicit val lightUser = Json.format[LightUser]
  implicit val formatChildComment = Json.format[ChildComment]

  import reactivemongo.bson._

  private[activity] implicit val BSONReaderChildComment = new BSONDocumentReader[ChildComment] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }
    def read(doc: BSONDocument): ChildComment = {
      ChildComment(
        id = ~doc.getAs[String]("_id"),
        parentId = ~doc.getAs[String]("parentId"),
        comment = ~doc.getAs[String]("comment"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("userId")).head,
        time = doc.getAs[DateTime]("time").head,
        likeCount = ~doc.getAs[Int]("likeCount"),
        likes = doc.getAs[List[String]]("likes")
      )
    }
  }
}

private[activity] object Comment {

  import reactivemongo.bson.Macros
  import lila.db.BSON
  implicit val lightUser = Json.format[LightUser]
  implicit val formatComment = Json.format[Comment]

  import reactivemongo.bson._

  private[activity] implicit val BSONReaderComment = new BSONDocumentReader[Comment] {
    implicit object BSONDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
      def read(time: BSONDateTime) = new DateTime(time.value)
      def write(jdtime: DateTime) = BSONDateTime(jdtime.getMillis)
    }

    implicit val formatChildComment = Json.format[ChildComment]
    def read(doc: BSONDocument): Comment = {
      Comment(
        id = ~doc.getAs[String]("_id"),
        parentPost = ~doc.getAs[String]("parentPost"),
        comment = ~doc.getAs[String]("comment"),
        user = lila.user.Env.current.lightUserApi.get(~doc.getAs[String]("userId")).head,
        time = doc.getAs[DateTime]("time").head,
        likeCount = ~doc.getAs[Int]("likeCount"),
        likes = doc.getAs[List[String]]("likes"),
        childCount = ~doc.getAs[Int]("childCount"),
        children = doc.getAs[List[ChildComment]]("children").get
      )
    }
  }
}


