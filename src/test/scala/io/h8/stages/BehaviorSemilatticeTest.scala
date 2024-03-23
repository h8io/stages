package io.h8.stages

import cats.Eq
import cats.kernel.Semilattice
import cats.kernel.laws.discipline.SemilatticeTests
import io.h8.stages.util.Identity
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FlatSpecDiscipline

class BehaviorSemilatticeTest extends AnyFlatSpec with Configuration with FlatSpecDiscipline {
  implicit private val BehaviorArbitrary: Arbitrary[Behavior[String, String]] = Arbitrary(
    Gen.oneOf(Behavior.Complete, Behavior.Redo(Identity[String]), Behavior.Undefined(Identity[String])))

  implicit private object BehaviorSemilattice extends Semilattice[Behavior[String, String]] {
    override def combine(x: Behavior[String, String], y: Behavior[String, String]): Behavior[String, String] =
      (x combine y)((_, _) => Identity[String])
  }

  implicit private val BehaviorEq: Eq[Behavior[String, String]] = Eq.fromUniversalEquals[Behavior[String, String]]
  checkAll("Semilattice[Behavior]", SemilatticeTests[Behavior[String, String]].semilattice)
}
