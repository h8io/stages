package h8io.xi.stages.examples

import h8io.xi.stages.*
import h8io.xi.stages.alterations.Repeat

object Factorial3 {
  trait FactorialError

  object NegativeNumberError extends FactorialError

  final case class Factorial(i: Int, factorial: BigInt) extends Stage[Int, BigInt, FactorialError] {
    override def apply(in: Int): Yield[Int, BigInt, FactorialError] =
      if (in < 0) Yield.None(Signal.Error(NegativeNumberError), InitialStage)
      else if (in < 2) Yield.Some(One, Signal.Complete, InitialStage)
      else if (in > i) Yield.Some(factorial, Signal.Success, this)
      else Yield.Some(factorial * i, Signal.Complete, InitialStage)

    override def onSuccess(): Stage[Int, BigInt, FactorialError] = Factorial(i + 1, factorial * i)
    override def onComplete(): Stage[Int, BigInt, FactorialError] = InitialStage
    override def onError(): Stage[Int, BigInt, FactorialError] = InitialStage
  }

  val InitialStage: Factorial = Factorial(2, One)

  val stage: Stage[Int, BigInt, FactorialError] = Repeat[Int, BigInt, FactorialError] _ <| InitialStage
}
