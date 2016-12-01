package lila.vocab

import org.joda.time.DateTime
import play.api.libs.json._

private[vocab] case class Vocab(
                                id: Int,
                                typeNum: Int,
                                question: String,
                                answers: String,
                                key: String
                              )

private[vocab] object Vocab {

  implicit val formatVocab = Json.format[Vocab]

  import reactivemongo.bson._

  private[vocab] implicit val BSONReaderVocab = new BSONDocumentReader[Vocab] {

    def read(doc: BSONDocument): Vocab = {
      Vocab(
        id = ~doc.getAs[Int]("_id"),
        typeNum = ~doc.getAs[Int]("typeNum"),
        question = ~doc.getAs[String]("question"),
        answers = ~doc.getAs[String]("answers"),
        key = ~doc.getAs[String]("key")
      )
    }
  }
}