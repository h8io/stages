package h8io.stages.binops

import h8io.stages.*
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
    with StagesCoreTestUtil {
  "And" should "return Yield.None if left stage returns Yield.None" in
    forAll(Gen.zip(Gen.long, Arbitrary.arbitrary[EvolutionToYieldNone[Long, Duration, Exception]])) {
      case (in, leftYieldSupplier) =>
        val leftStage = mock[Stage[Long, Duration, Exception]]("left stage")
        val rightStage = mock[Stage[Long, Instant, Exception]]("right stage")
        val leftYield = leftYieldSupplier(mock[Evolution[Long, Duration, Exception]]("left evolution"))
        (leftStage.apply _).expects(in).returns(leftYield)
        inside(And(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          status shouldBe leftYield.status
          testWrappedEvolution(evolution, leftYield.evolution, And(_: Stage[Long, Duration, Exception], rightStage))
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
        val rightStage = mock[Stage[UUID, ZoneId, String]]("right stage")
        val leftYield = leftYieldSupplier(mock[Evolution[UUID, Long, String]]("left evolution"))
        val rightYield = rightYieldSupplier(mock[Evolution[UUID, ZoneId, String]]("right Evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(And(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          test(leftYield, rightYield, status, evolution)
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
        val rightStage = mock[Stage[ZoneOffset, LocalDate, Short]]("right stage")
        val leftYield = leftYieldSupplier(mock[Evolution[ZoneOffset, OffsetDateTime, Short]]("left evolution"))
        val rightYield = rightYieldSupplier(mock[Evolution[ZoneOffset, LocalDate, Short]]("right Evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(And(leftStage, rightStage)(in)) { case Yield.Some(out, status, evolution) =>
          out shouldBe leftYield.out -> rightYield.out
          test(leftYield, rightYield, status, evolution)
        }
    }

  private def test[I, LO, RO, E](
      leftYield: Yield[I, LO, E],
      rightYield: Yield[I, RO, E],
      status: Status[E],
      evolution: Evolution[I, (LO, RO), E]): Assertion = {
    status shouldBe leftYield.status ++ rightYield.status

    val leftOnSuccessStage = mock[Stage[I, LO, E]]("left onSuccess stage")
    val rightOnSuccessStage = mock[Stage[I, RO, E]]("right onSuccess stage")
    inSequence {
      (leftYield.evolution.onSuccess _).expects().returns(leftOnSuccessStage)
      (rightYield.evolution.onSuccess _).expects().returns(rightOnSuccessStage)
    }
    evolution.onSuccess() shouldBe And(leftOnSuccessStage, rightOnSuccessStage)

    val leftOnCompleteStage = mock[Stage[I, LO, E]]("left onComplete stage")
    val rightOnCompleteStage = mock[Stage[I, RO, E]]("right onComplete stage")
    inSequence {
      (leftYield.evolution.onComplete _).expects().returns(leftOnCompleteStage)
      (rightYield.evolution.onComplete _).expects().returns(rightOnCompleteStage)
    }
    evolution.onComplete() shouldBe And(leftOnCompleteStage, rightOnCompleteStage)

    val leftOnErrorStage = mock[Stage[I, LO, E]]("left onError stage")
    val rightOnErrorStage = mock[Stage[I, RO, E]]("right onError stage")
    inSequence {
      (leftYield.evolution.onError _).expects().returns(leftOnErrorStage)
      (rightYield.evolution.onError _).expects().returns(rightOnErrorStage)
    }
    evolution.onError() shouldBe And(leftOnErrorStage, rightOnErrorStage)
  }
}
