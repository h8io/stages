package h8io.stages.examples

import h8io.stages.*
import h8io.stages.operators.{And, Loop}
import h8io.stages.base.BaseEvolution
import h8io.stages.projections.Tuple2
import h8io.stages.std.{Const, Identity}

object Factorial2 {
  /*
   * Example 2: factorial with explicit (n, acc) state.
   * The input n is paired with an accumulator, then a loop decrements n and
   * multiplies acc until completion. Negative input yields an error status.
   * The final stage projects the accumulator out of the tuple.
   */
  object Agg extends Stage.Endo[(Int, BigInt), String] with BaseEvolution.Endo[(Int, BigInt), String] {
    override def apply(in: (Int, BigInt)): Yield[(Int, BigInt), (Int, BigInt), String] =
      if (in._1 > 1) Yield.Some((in._1 - 1, in._2 * in._1), Status.Success, this)
      else if (in._1 < 0) Yield.None(Status.Error("negative number"), this)
      else Yield.Some(in, Status.Complete, this)
  }

  val stage: Stage[Int, BigInt, String] =
    And(Identity[Int], Const(One)) ~> Loop[(Int, BigInt), String](Agg) ~> Tuple2.Right[BigInt]
}
