package lila.activity

import lila.common.LightUser
import lila.user.LightUserApi
import lila.db.BSON
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {

  implicit val postBSONHandler = new BSON[Post] {
    def reads(r: BSON.Reader) = {
      Post(
        id = r str "_id",
        content = r str "content",
        user = lila.user.Env.current.lightUserApi.get(r str "userId").getOrElse(LightUser("", "", None, "")),
        published = r date "published",
        likeCount = r int "likeCount",
        likes = r getO[List[String]] "likes",
        shareCount = r int "shareCount",
        shares = r getO[List[String]] "shares",
        commentCount = r int "commentCount"
      )
    }
    def writes(w: BSON.Writer, o: Post) = {
      BSONDocument(
        "_id" -> o.id,
        "content" -> o.content,
        "userId" -> o.user.id,
        "published" -> w.date(o.published),
        "likeCount" -> o.likeCount,
        "likes" -> o.likes,
        "shareCount" -> o.shareCount,
        "shares" -> o.shares,
        "commentCount" -> o.commentCount
      )
    }
  }

  implicit val childCommentBSONHandler = new BSON[ChildComment] {
    def reads(r: BSON.Reader) = {
      ChildComment(
        id = r str "_id",
        parentId = r str "parentId",
        comment = r str "comment",
        user = lila.user.Env.current.lightUserApi.get(r str "userId").getOrElse(LightUser("", "", None, "")),
        time = r date "time",
        likeCount = r int "likeCount",
        likes = r getO[List[String]] "likes"
      )
    }
    def writes(w: BSON.Writer, o: ChildComment) = {
      BSONDocument(
        "_id" -> o.id,
        "parentId" -> o.parentId,
        "comment" -> o.comment,
        "userId" -> o.user.id,
        "time" -> w.date(o.time),
        "likeCount" -> o.likeCount,
        "likes" -> o.likes
      )
    }
  }

  implicit val commentBSONHandler = new BSON[Comment] {

    def reads(r: BSON.Reader) = {
      Comment(
        id = r str "_id",
        parentPost = r str "parentPost",
        comment = r str "comment",
        user = lila.user.Env.current.lightUserApi.get(r str "userId").get,
        time = r date "time",
        likeCount = r int "likeCount",
        likes = r getO[List[String]] "likes",
        childCount = r int "childCount",
        children = r get[List[ChildComment]] "children"
      )
    }

    def writes(w: BSON.Writer, o: Comment) = {
      BSONDocument(
        "_id" -> o.id,
        "parentPost" -> o.parentPost,
        "comment" -> o.comment,
        "userId" -> o.user.id,
        "time" -> w.date(o.time),
        "likeCount" -> o.likeCount,
        "likes" -> o.likes,
        "childCount" -> o.childCount,
        "children" -> o.children
      )
    }
  }
}