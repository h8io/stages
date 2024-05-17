package io.h8.stages

import cats.kernel.laws.discipline.SemilatticeTests
import cats.kernel.{Eq, Semilattice}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FlatSpecDiscipline

class ConclusionTest extends AnyFlatSpec with Configuration with FlatSpecDiscipline {
  implicit private val BehaviorArbitrary: Arbitrary[Conclusion[String, String]] = Arbitrary(
    Gen.oneOf(Conclusion.Complete, Conclusion.Redo(Identity[String]), Conclusion.Undefined(Identity[String])))
  implicit private val BehaviorSemilattice: Semilattice[Conclusion[String, String]] = (x, y) =>
    (x combine y)((_, _) => Identity[String])
  implicit private val BehaviorEq: Eq[Conclusion[String, String]] = Eq.fromUniversalEquals[Conclusion[String, String]]

  checkAll("Semilattice[Conclusion]", SemilatticeTests[Conclusion[String, String]].semilattice)
}
