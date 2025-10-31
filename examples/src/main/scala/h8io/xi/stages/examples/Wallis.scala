package h8io.xi.stages.examples

import h8io.xi.stages.*
import h8io.xi.stages.alterations.Loop
import h8io.xi.stages.std.{Const, GlobalSoftDeadline}

import scala.concurrent.duration.FiniteDuration

object Wallis {
  final case class Pi(n: Long) extends Stage.Endo[Double, Nothing] {
    def apply(in: Double): Yield.Some[Double, Double, Nothing] = {
      val k = 4d * n * n
      Yield.Some(in * k / (k - 1), Signal.Success, this)
    }

    override def onSuccess(): Stage[Double, Double, Nothing] = Pi(n + 1)
    override def onComplete(): Stage[Double, Double, Nothing] = InitialStage
    override def onError(): Stage[Double, Double, Nothing] = InitialStage
  }

  val InitialStage = Pi(1)

  def stage(duration: FiniteDuration): Stage[Any, Double, Nothing] =
    Const(2d) ~> Loop[Double, Nothing] _ <| InitialStage ~> GlobalSoftDeadline(duration)
}
