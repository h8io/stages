package h8io.stages

import cats.kernel.laws.discipline.MonoidTests
import cats.{Eq, Monoid}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class StatusMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers with StagesCoreArbitraries {
  private implicit def statusMonoid[E]: Monoid[Status[E]] =
    new Monoid[Status[E]] {
      def empty: Status[E] = Status.Success
      def combine(x: Status[E], y: Status[E]): Status[E] = x ++ y
    }

  private implicit def statusEq[E]: Eq[Status[E]] = Eq.fromUniversalEquals[Status[E]]

  checkAll("Status[Int]", MonoidTests[Status[Int]].monoid)
}
