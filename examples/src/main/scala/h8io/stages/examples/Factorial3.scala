package h8io.stages.examples

import h8io.stages.*
import h8io.stages.base.{BaseStage, StageOps}
import h8io.stages.operators.Repeat

object Factorial3 {
  /*
   * Example 3: factorial as a stateful stage that evolves itself.
   * The stage carries the current multiplier and accumulated value. Each Success
   * advances the state, and Repeat keeps invoking it until it signals Complete.
   * Negative input is translated into a domain-specific error.
   */
  trait FactorialError

  object NegativeNumberError extends FactorialError

  final case class Factorial(i: Int, factorial: BigInt) extends BaseStage[Int, BigInt, FactorialError] {
    override def apply(in: Int): Yield[Int, BigInt, FactorialError] =
      if (in < 0) Yield.None(Status.error(NegativeNumberError), InitialStage.toEvolution)
      else if (in < 2) Yield.Some(One, Status.complete, InitialStage.toEvolution)
      else if (in >= i) Yield.Some(
        factorial,
        Status.Success,
        new Evolution[Int, BigInt, FactorialError] {
          override def apply(status: Status[?]): Stage[Int, BigInt, FactorialError] =
            status match {
              case Status.Success => Factorial(i + 1, factorial * i)
              case Status.Complete(_) => InitialStage
            }

          override def dispose(): Unit = ()
        }
      )
      else Yield.Some(factorial, Status.complete, InitialStage.toEvolution)
  }

  val InitialStage: Factorial = Factorial(2, One)

  val pipeline: Stage[Int, BigInt, FactorialError] = Repeat[Int, BigInt, FactorialError](InitialStage)
}
