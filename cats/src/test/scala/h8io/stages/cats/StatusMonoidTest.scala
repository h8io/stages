package h8io.stages.cats

import cats.kernel.laws.discipline.MonoidTests
import h8io.stages.{StagesCoreArbitraries, Status}
import h8io.stages.cats.StatusInstances.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.discipline.scalatest.FunSuiteDiscipline

class StatusMonoidTest extends AnyFunSuite with FunSuiteDiscipline with Checkers with StagesCoreArbitraries {
  checkAll("Status[Int]", MonoidTests[Status[Int]].monoid)
}
