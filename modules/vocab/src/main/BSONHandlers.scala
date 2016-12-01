package lila.vocab

import lila.db.BSON
import reactivemongo.bson._
import org.joda.time.DateTime

object BSONHandlers {

  implicit val vocabBSONHandler = new BSON[Vocab] {
    def reads(r: BSON.Reader) = {
      Vocab(
        id = r int "_id",
        typeNum = r int "typeNum",
        question = r str "question",
        answers = r str "answers",
        key = r str "key"
      )
    }
    def writes(w: BSON.Writer, o: Vocab) = {
      BSONDocument(
        "_id" -> o.id,
        "typeNum" -> o.typeNum,
        "question" -> o.question,
        "answers" -> o.answers,
        "key" -> o.key
      )
    }
  }

}