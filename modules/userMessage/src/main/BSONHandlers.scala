package lila.userMessage

import lila.common.LightUser
import lila.user.LightUserApi
import lila.db.BSON
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {

  implicit val messageBSONHandler = new BSON[UserMessage] {
    def reads(r: BSON.Reader) = {
      UserMessage(
        //_id = r str "_id",
        //mid = r str "mid",
        mv = r int "mv",
        f = lila.user.Env.current.lightUserApi.get(r str "f").head,
        t = lila.user.Env.current.lightUserApi.get(r str "t").head,
        mes = r str "mes",
        time = r date "time")
    }
    def writes(w: BSON.Writer, o: UserMessage) = {
      val mid = if(o.f.id < o.t.id) o.f.id + o.t.id else o.t.id + o.f.id
      BSONDocument(
        //"_id" -> o._id,
        "mid" -> mid,
        "mv" -> o.mv,
        "f" -> o.f.id,
        "t" -> o.t.id,
        "mes" -> o.mes,
        "time" -> w.date(o.time))
    }
  }

  implicit val notifyMessageBSONHandler = new BSON[NotifyMessage] {
    def reads(r: BSON.Reader) = {
      NotifyMessage(
        user = lila.user.Env.current.lightUserApi.get(r str "uid").head,
        n = r int "n",
        d = r date "d",
        lm = r get[UserMessage] "lm"
      )
    }

    def writes(w: BSON.Writer, o: NotifyMessage) = {
      BSONDocument(
        "uid" -> o.user.id,
        "n" -> o.n,
        "d" -> w.date(o.d),
        "lm" -> o.lm
      )
    }
  }
  implicit val notifyBSONHandler = new BSON[Notify] {
    def reads(r: BSON.Reader) = {
      Notify(
        id = r str "_id",
        m = r get[List[NotifyMessage]] "m",
        n = r int "n",
        ur = r get[List[String]] "ur"
      )
    }

    def writes(w: BSON.Writer, o: Notify) = {
      BSONDocument(
        "_id" -> o.id,
        "m" -> o.m,
        "n" -> o.n,
        "ur" -> o.ur
      )
    }
  }

}