package io.h8.stages

import cats.kernel.laws.discipline.SemilatticeTests
import cats.kernel.{Eq, Semilattice}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FlatSpecDiscipline

class BehaviorKindTest extends AnyFlatSpec with Configuration with FlatSpecDiscipline {
  implicit private val BehaviorKindArbitrary: Arbitrary[BehaviorKind] = Arbitrary(Gen.oneOf(BehaviorKind.values))
  implicit private val BehaviorKindSemilattice: Semilattice[BehaviorKind] = (x, y) => x & y
  implicit private val BehaviorKindEq: Eq[BehaviorKind] = Eq.fromUniversalEquals[BehaviorKind]

  checkAll("Lattice[BehaviorKind]", SemilatticeTests[BehaviorKind].semilattice)
}
