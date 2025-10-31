package h8io.xi.stages.examples

import h8io.xi.stages.*
import h8io.xi.stages.alterations.{LocalSoftDeadline, Repeat}

import scala.concurrent.duration.FiniteDuration

object Leibniz {
  final case class Pi(n: Long, t: Double, s: Double) extends Stage[Unit, Double, Nothing] {
    def apply(in: Unit): Yield.Some[Unit, Double, Nothing] = Yield.Some(4 * s, Signal.Success, this)

    override def onSuccess(): Stage[Unit, Double, Nothing] = {
      val _t = -t * (2 * n + 1) / (2 * n + 3)
      Pi(n + 1, _t, s + _t)
    }
    override def onComplete(): Stage[Unit, Double, Nothing] = InitialStage
    override def onError(): Stage[Unit, Double, Nothing] = InitialStage
  }

  val InitialStage: Pi = Pi(0, 1, 1)

  def stage1(duration: FiniteDuration): Stage[Unit, Double, Nothing] =
    Repeat[Unit, Double, Nothing] _ ~> LocalSoftDeadline[Unit, Double, Nothing](duration) <| InitialStage

  def stage2(duration: FiniteDuration): Stage[Unit, Double, Nothing] =
    Repeat[Unit, Double, Nothing] _ <| LocalSoftDeadline[Unit, Double, Nothing](duration) <| InitialStage
}
