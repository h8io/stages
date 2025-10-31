package h8io.xi.stages.examples

import h8io.xi.stages.*
import h8io.xi.stages.alterations.Loop
import h8io.xi.stages.binops.And
import h8io.xi.stages.std.{Const, Identity}

object Factorial2 {
  object Agg extends Stage.Endo[(Int, BigInt), String] {
    override def apply(in: (Int, BigInt)): Yield[(Int, BigInt), (Int, BigInt), String] =
      if (in._1 > 1) Yield.Some((in._1 - 1, in._2 * in._1), Signal.Success, this)
      else if (in._1 < 0) Yield.None(Signal.Error("negative number"), this)
      else Yield.Some(in, Signal.Complete, this)
  }

  val stage: Stage[Int, BigInt, String] =
    And(Identity[Int], Const(One)) ~> Loop[(Int, BigInt), String] _ ⋅ Agg ~> projections.Tuple2.Right[BigInt]
}
