package io.h8.stages

import cats.kernel.laws.discipline.SemilatticeTests
import cats.kernel.{Eq, Semilattice}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FlatSpecDiscipline

class BehaviorKindTest extends AnyFlatSpec with Configuration with FlatSpecDiscipline {
  implicit private val BehaviorKindArbitrary: Arbitrary[BehaviorKind] = Arbitrary(Gen.oneOf(BehaviorKind.values))

  implicit private object BehaviorKindSemilattice extends Semilattice[BehaviorKind] {
    override def combine(x: BehaviorKind, y: BehaviorKind): BehaviorKind = x & y
  }

  implicit private val BehaviorKindEq: Eq[BehaviorKind] = Eq.fromUniversalEquals[BehaviorKind]

  checkAll("Lattice[BehaviorKind]", SemilatticeTests[BehaviorKind].semilattice)
}
