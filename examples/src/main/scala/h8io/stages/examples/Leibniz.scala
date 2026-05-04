package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.BaseStage
import h8io.stages.operators.{LocalSoftDeadline, Repeat}

import scala.concurrent.duration.FiniteDuration

object Leibniz {
  /*
   * Example: Pi approximation via the Leibniz series.
   * Each step emits the current approximation and evolves to the next term.
   * A local soft deadline stops the run after the requested duration, and Repeat
   * keeps stepping until completion.
   */
  final case class Pi(n: Long, t: Double, s: Double) extends BaseStage[Unit, Double, Nothing] {
    override def apply(in: Unit): Yield.Some[Unit, Double, Nothing] =
      Yield.Some(
        4 * s,
        Status.Success,
        new Evolution[Unit, Double, Nothing] {
          override def apply(status: Status[?]): Stage[Unit, Double, Nothing] =
            status match {
              case Status.Success =>
                val _t = -t * (2 * n + 1) / (2 * n + 3)
                Pi(n + 1, _t, s + _t)
              case Status.Complete(_) => InitialStage
            }

          override def dispose(): Unit = ()
        }
      )
  }

  val InitialStage: Pi = Pi(0, 1, 1)

  def pipeline(duration: FiniteDuration): Stage[Unit, Double, Nothing] =
    Repeat[Unit, Double, Nothing](LocalSoftDeadline[Unit, Double, Nothing](duration)(InitialStage))
}
