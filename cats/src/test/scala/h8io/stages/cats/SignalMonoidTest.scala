package h8io.stages.cats

import cats.kernel.laws.discipline.MonoidTests
import cats.{Eq, Monoid}
import h8io.stages.{Signal, StagesCoreArbitraries}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class SignalMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers with StagesCoreArbitraries {
  private implicit def signalMonoid[E]: Monoid[Signal[E]] =
    new Monoid[Signal[E]] {
      def empty: Signal[E] = Signal.Success
      def combine(x: Signal[E], y: Signal[E]): Signal[E] = x ++ y
    }

  private implicit def signalEq[E]: Eq[Signal[E]] = Eq.fromUniversalEquals[Signal[E]]

  checkAll("Signal[Int]", MonoidTests[Signal[Int]].monoid)
}
