package h8io.xi.stages.examples

import h8io.xi.stages.*
import h8io.xi.stages.alterations.Loop
import h8io.xi.stages.std.{Const, Countdown}

object Factorial1 {
  sealed case class Agg(n: Long) extends Stage.Endo[BigInt, Nothing] {
    override def apply(in: BigInt): Yield.Some[BigInt, BigInt, Nothing] = Yield.Some(in * n, Signal.Success, this)

    override def onSuccess(): Stage.Endo[BigInt, Nothing] = Agg(n + 1)
    override def onComplete(): Stage.Endo[BigInt, Nothing] = Agg
    override def onError(): Stage.Endo[BigInt, Nothing] = Agg
  }

  object Agg extends Agg(1)

  def stage(n: Int): Stage[Unit, BigInt, Nothing] = Const(One) ~> Loop[BigInt, Nothing] _ <| Agg ~> Countdown[BigInt](n)
}
