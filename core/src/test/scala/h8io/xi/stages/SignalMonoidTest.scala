package h8io.xi.stages

import cats.Eq
import cats.kernel.laws.discipline.MonoidTests
import h8io.xi.stages.test.signalMonoid
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class SignalMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers with StagesCoreArbitraries {
  private implicit def signalEq[E]: Eq[Signal[E]] = Eq.fromUniversalEquals[Signal[E]]

  checkAll("Signal[Int]", MonoidTests[Signal[Int]].monoid)
}
