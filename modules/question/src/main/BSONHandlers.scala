package lila.question

import lila.common.LightUser
import lila.user.LightUserApi
import lila.db.BSON
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {

  implicit val voteBSONHandler = new BSON[Vote] {
    def reads(r: BSON.Reader) = {
      Vote(
        userId = r str "userId",
        vote = r int "vote"
      )
    }
    def writes(w: BSON.Writer, o: Vote) = {
      BSONDocument(
        "userId" -> o.userId,
        "vote" -> o.vote
      )
    }
  }

  implicit val dayViewsBSONHandler = new BSON[DayViews] {
    def reads(r: BSON.Reader) = {
      DayViews(
        date = r date "date",
        views = r int "views"
      )
    }
    def writes(w: BSON.Writer, o: DayViews) = {
      BSONDocument(
        "date" -> w.date(o.date),
        "views" -> o.views
      )
    }
  }

  implicit val commentBSONHandler = new BSON[Comment] {
    def reads(r: BSON.Reader) = {
      Comment(
        id = r str "_id",
        parentId = r str "parentId",
        comment = r str "comment",
        user = lila.user.Env.current.lightUserApi.get(r str "userId").getOrElse(LightUser("", "", None, "")),
        time = r date "time"
      )
    }
    def writes(w: BSON.Writer, o: Comment) = {
      BSONDocument(
        "_id" -> o.id,
        "parentId" -> o.parentId,
        "comment" -> o.comment,
        "userId" -> o.user.id,
        "time" -> w.date(o.time)
      )
    }
  }

  implicit val questionBSONHandler = new BSON[Question] {
    def reads(r: BSON.Reader) = {
      Question(
        id = r str "_id",
        question = r str "question",
        description = r str "description",
        user = lila.user.Env.current.lightUserApi.get(r str "userId").getOrElse(LightUser("", "", None, "")),
        published = r date "published",
        views = r int "views",
        dayViews = r getO[List[DayViews]] "dayViews",
        voteCount = r int "voteCount",
        votes = r getO[List[Vote]] "votes",
        shareCount = r int "shareCount",
        shares = r get[List[String]] "shares",
        commentCount = r int "commentCount",
        comment = r getO[List[Comment]] "comment",
        answerCount = r int "answerCount"
      )
    }
    def writes(w: BSON.Writer, o: Question) = {
      BSONDocument(
        "_id" -> o.id,
        "question" -> o.question,
        "description" -> o.description,
        "userId" -> o.user.id,
        "published" -> w.date(o.published),
        "views" -> o.views,
        "dayViews" -> o.dayViews,
        "voteCount" -> o.voteCount,
        "votes" -> o.votes,
        "shareCount" -> o.shareCount,
        "shares" -> o.shares,
        "commentCount" -> o.commentCount,
        "comment" -> o.comment,
        "answerCount" -> o.answerCount
      )
    }
  }


  implicit val answerBSONHandler = new BSON[Answer] {
    def reads(r: BSON.Reader) = {
      Answer(
        id = r str "_id",
        qId = r str "qId",
        answer = r str "answer",
        user = lila.user.Env.current.lightUserApi.get(r str "userId").getOrElse(LightUser("", "", None, "")),
        published = r date "published",
        voteCount = r int "voteCount",
        votes = r getO[List[Vote]] "votes",
        commentCount = r int "commentCount",
        comment = r getO[List[Comment]] "comment"
      )
    }

    def writes(w: BSON.Writer, o: Answer) = {
      BSONDocument(
        "_id" -> o.id,
        "qId" -> o.qId,
        "answer" -> o.answer,
        "userId" -> o.user.id,
        "published" -> w.date(o.published),
        "voteCount" -> o.voteCount,
        "votes" -> o.votes,
        "commentCount" -> o.commentCount,
        "comment" -> o.comment
      )
    }
  }
}