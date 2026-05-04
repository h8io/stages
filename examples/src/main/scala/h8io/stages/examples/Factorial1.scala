package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.BaseStage
import h8io.stages.operators.Loop
import h8io.stages.std.{Const, Countdown}

object Factorial1 {
  /*
   * Example 1: factorial as a small pipeline.
   * The pipeline seeds the accumulator with 1, then loops a stage that multiplies
   * by an internal counter and a countdown that stops after n steps.
   * Input is Unit, output is the computed factorial.
   */
  sealed case class Agg(n: Long) extends BaseStage.Endo[BigInt, Nothing] {
    override def apply(in: BigInt): Yield.Some[BigInt, BigInt, Nothing] =
      Yield.Some(
        in * n,
        Status.Success,
        new Evolution.Endo[BigInt, Nothing] {
          override def apply(status: Status[?]): Stage[BigInt, BigInt, Nothing] =
            status match {
              case Status.Success => Agg(n + 1)
              case Status.Complete(_) => Agg
            }
          override def dispose(): Unit = ()
        }
      )
  }

  object Agg extends Agg(1)

  def pipeline(n: Int): Stage[Unit, BigInt, Nothing] =
    Const(One) ~> Loop[BigInt, Nothing](Agg ~> Countdown[BigInt](n))
}
