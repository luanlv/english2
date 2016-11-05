package lila.hub

import akka.pattern.ask
import play.api.data._
import actorApi.captcha._
import lila.common.{Captcha, Captcha2}

trait CaptchedForm {

  import makeTimeout.large

  type CaptchedData = {
    def gameId: String
    def move: String
  }

  def captcher: akka.actor.ActorSelection

  def anyCaptcha: Fu[Captcha2] =
    (captcher ? AnyCaptcha).mapTo[Captcha2]

  def getCaptcha(id: String): Fu[Captcha] =
    (captcher ? GetCaptcha(id)).mapTo[Captcha]

  def withCaptcha[A](form: Form[A]): Fu[(Form[A], Captcha2)] =
    anyCaptcha map (form -> _)

  def validateCaptcha(data: CaptchedData) =
    getCaptcha(data.gameId) awaitSeconds 2 valid data.move.trim.toLowerCase

  val captchaFailMessage = "captcha.fail"
}
