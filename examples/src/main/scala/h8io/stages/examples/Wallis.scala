package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.SAMStage
import h8io.stages.operators.Loop
import h8io.stages.std.{Const, GlobalSoftDeadline}

import scala.concurrent.duration.FiniteDuration

object Wallis {
  /*
   * Example: Pi approximation via the Wallis product.
   * Starts from 2.0 and multiplies by successive terms; a global soft deadline
   * stops the loop after the requested duration.
   */
  final case class Pi(n: Long) extends SAMStage.Endo[Double, Nothing] {
    override def apply(in: Double): Yield.Some[Double, Double, Nothing] = {
      val k = 4d * n * n
      Yield.Some(
        in * k / (k - 1),
        Status.Success,
        new Evolution.Endo[Double, Nothing] {
          override def evolve(status: Status[?]): Stage[Double, Double, Nothing] =
            status match {
              case Status.Success => Pi(n + 1)
              case Status.Complete(_) => InitialStage
            }
          override def dispose(): Unit = ()
        }
      )
    }
  }

  val InitialStage = Pi(1)

  def pipeline(duration: FiniteDuration): Stage[Any, Double, Nothing] =
    Const(2d) ~> Loop[Double, Nothing](InitialStage ~> GlobalSoftDeadline(duration))
}
