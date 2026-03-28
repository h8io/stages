package h8io.stages.examples

import h8io.stages.*
import h8io.stages.operators.Loop
import h8io.stages.base.BaseEvolution
import h8io.stages.std.{Const, Countdown}

object Factorial1 {
  /*
   * Example 1: factorial as a small pipeline.
   * The pipeline seeds the accumulator with 1, then loops a stage that multiplies
   * by an internal counter and a countdown that stops after n steps.
   * Input is Unit, output is the computed factorial.
   */
  sealed case class Agg(n: Long) extends Stage.Endo[BigInt, Nothing] with BaseEvolution.Endo[BigInt, Nothing] {
    override def apply(in: BigInt): Yield.Some[BigInt, BigInt, Nothing] = Yield.Some(in * n, Status.Success, this)

    override def onSuccess(): Stage.Endo[BigInt, Nothing] = Agg(n + 1)
    override def onComplete(): Stage.Endo[BigInt, Nothing] = Agg
    override def onError(): Stage.Endo[BigInt, Nothing] = Agg
  }

  object Agg extends Agg(1)

  def stage(n: Int): Stage[Unit, BigInt, Nothing] = Const(One) ~> Loop[BigInt, Nothing](Agg ~> Countdown[BigInt](n))
}
