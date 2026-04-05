package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.StagesBaseTestUtil
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.*
import java.util.UUID
import scala.concurrent.duration.Duration

class AndTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesBaseTestUtil {
  "And" should "return Yield.None if left stage returns Yield.None" in
    forAll(Gen.zip(Gen.long, Arbitrary.arbitrary[EvolutionToYieldNone[Long, Duration, Exception]])) {
      case (in, leftYieldSupplier) =>
        val leftStage = mock[Stage[Long, Duration, Exception]]("left stage")
        val leftYield = leftYieldSupplier(mock[Evolution[Long, Duration, Exception]]("left evolution"))
        val rightStage = mock[Stage[Long, Instant, Exception]]("right stage")
        val rightEvolution = mock[Evolution[Long, Instant, Exception]]("right evolution")
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.skip _).expects().returns(rightEvolution)
        }
        inside(And(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          status shouldBe leftYield.status
          test(leftYield.evolution, rightEvolution, evolution)
        }
    }

  it should "return Yield.None if the left stage returns Yield.Some and the right one returns Yield.None" in
    forAll(
      Gen.zip(
        Gen.uuid,
        Arbitrary.arbitrary[EvolutionToYieldSome[UUID, Long, String]],
        Arbitrary.arbitrary[EvolutionToYieldNone[UUID, ZoneId, String]])) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[UUID, Long, String]]("left stage")
        val leftYield = leftYieldSupplier(mock[Evolution[UUID, Long, String]]("left evolution"))
        val rightStage = mock[Stage[UUID, ZoneId, String]]("right stage")
        val rightYield = rightYieldSupplier(mock[Evolution[UUID, ZoneId, String]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(And(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          status shouldBe leftYield.status ++ rightYield.status
          test(leftYield.evolution, rightYield.evolution, evolution)
        }
    }

  it should "return a tuple output if both stages return Yield.Some" in
    forAll(
      Gen.zip(
        Arbitrary.arbitrary[ZoneOffset],
        Arbitrary.arbitrary[EvolutionToYieldSome[ZoneOffset, OffsetDateTime, Short]],
        Arbitrary.arbitrary[EvolutionToYieldSome[ZoneOffset, LocalDate, Short]]
      )) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[ZoneOffset, OffsetDateTime, Short]]("left stage")
        val leftYield = leftYieldSupplier(mock[Evolution[ZoneOffset, OffsetDateTime, Short]]("left evolution"))
        val rightStage = mock[Stage[ZoneOffset, LocalDate, Short]]("right stage")
        val rightYield = rightYieldSupplier(mock[Evolution[ZoneOffset, LocalDate, Short]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(And(leftStage, rightStage)(in)) { case Yield.Some(out, status, evolution) =>
          out shouldBe leftYield.out -> rightYield.out
          status shouldBe leftYield.status ++ rightYield.status
          test(leftYield.evolution, rightYield.evolution, evolution)
        }
    }

  private def test[I, LO, RO, E](
      leftEvolution: Evolution[I, LO, E],
      rightEvolution: Evolution[I, RO, E],
      evolution: Evolution[I, (LO, RO), E]): Assertion = {
    val leftOnSuccessStage = mock[Stage[I, LO, E]]("left onSuccess stage")
    val rightOnSuccessStage = mock[Stage[I, RO, E]]("right onSuccess stage")
    inSequence {
      (leftEvolution.onSuccess _).expects().returns(leftOnSuccessStage)
      (rightEvolution.onSuccess _).expects().returns(rightOnSuccessStage)
    }
    evolution.onSuccess() shouldBe And(leftOnSuccessStage, rightOnSuccessStage)

    val leftOnCompleteStage = mock[Stage[I, LO, E]]("left onComplete stage")
    val rightOnCompleteStage = mock[Stage[I, RO, E]]("right onComplete stage")
    inSequence {
      (leftEvolution.onComplete _).expects().returns(leftOnCompleteStage)
      (rightEvolution.onComplete _).expects().returns(rightOnCompleteStage)
    }
    evolution.onComplete() shouldBe And(leftOnCompleteStage, rightOnCompleteStage)

    val leftOnErrorStage = mock[Stage[I, LO, E]]("left onError stage")
    val rightOnErrorStage = mock[Stage[I, RO, E]]("right onError stage")
    inSequence {
      (leftEvolution.onError _).expects().returns(leftOnErrorStage)
      (rightEvolution.onError _).expects().returns(rightOnErrorStage)
    }
    evolution.onError() shouldBe And(leftOnErrorStage, rightOnErrorStage)
  }
}
