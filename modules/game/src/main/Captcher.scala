package lila.game

import lila.game.Captcher.SayHello

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.actor._
import akka.pattern.{ ask, pipe }

import scalaz.{ NonEmptyList, OptionT }

import lila.common.Captcha, Captcha._
import lila.common.Captcha2, Captcha2._
import lila.hub.actorApi.captcha._


object Captcher {
  def props = Props[Captcher]

  case class SayHello(name: String)
}


// only works with standard chess (not chess960)
 final class Captcher extends Actor {

  def receive = {

    case SayHello(name: String) => sender ! "Hello, " + name

    case AnyCaptcha             => {

      sender ! Captcha2.default
    }

    case GetCaptcha(id: String) =>  sender ! Captcha2.default

  }


 // Private stuff

    private val capacity = 512
    private var challenges: NonEmptyList[Captcha] = NonEmptyList(Captcha.default)

    private def add(c: Captcha) {
      find(c.gameId) ifNone {
        challenges = NonEmptyList.nel(c, challenges.list take capacity)
      }
    }

    private def find(id: String): Option[Captcha] =
      challenges.list.find(_.gameId == id)

}
