package h8io.stages.examples

import h8io.stages.*
import h8io.stages.alterations.Repeat
import h8io.stages.base.BaseEvolution

object Factorial3 {
  trait FactorialError

  object NegativeNumberError extends FactorialError

  final case class Factorial(i: Int, factorial: BigInt)
      extends Stage[Int, BigInt, FactorialError] with BaseEvolution[Int, BigInt, FactorialError] {
    override def apply(in: Int): Yield[Int, BigInt, FactorialError] =
      if (in < 0) Yield.None(Status.Error(NegativeNumberError), InitialStage)
      else if (in < 2) Yield.Some(One, Status.Complete, InitialStage)
      else if (in > i) Yield.Some(factorial, Status.Success, this)
      else Yield.Some(factorial * i, Status.Complete, InitialStage)

    override def onSuccess(): Stage[Int, BigInt, FactorialError] = Factorial(i + 1, factorial * i)
    override def onComplete(): Stage[Int, BigInt, FactorialError] = InitialStage
    override def onError(): Stage[Int, BigInt, FactorialError] = InitialStage
  }

  val InitialStage: Factorial = Factorial(2, One)

  val stage: Stage[Int, BigInt, FactorialError] = Repeat[Int, BigInt, FactorialError] _ <| InitialStage
}
