package lila.game



// times are expressed in seconds
case class CorrespondenceClock(
    increment: Int,
    whiteTime: Float,
    blackTime: Float) {

  def daysPerTurn = increment / 60 / 60 / 24

  def emerg = 60 * 10


  // in seconds
  def estimateTotalTime = increment * 40 / 2
}
