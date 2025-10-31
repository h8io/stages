package h8io.xi.stages.binops

import h8io.xi.stages.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.duration.Duration

class OrTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "Or" should "return Yield.None if both stages return Yield.None" in
    forAll(
      Gen.zip(Gen.long,
        Arbitrary.arbitrary[OnDoneToYieldNone[Long, Duration, Exception]],
        Arbitrary.arbitrary[OnDoneToYieldNone[Long, Instant, Exception]])) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[Long, Duration, Exception]]("left stage")
        val rightStage = mock[Stage[Long, Instant, Exception]]("right stage")
        val leftYield = leftYieldSupplier(mock[OnDone[Long, Duration, Exception]]("left onDone"))
        val rightYield = rightYieldSupplier(mock[OnDone[Long, Instant, Exception]]("right OnDone"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(Or(leftStage, rightStage)(in)) { case Yield.None(signal, onDone) =>
          test(leftYield, rightYield, signal, onDone)
        }
    }

  it should "return Right output if the left stage returns Yield.None and the right one returns Yield.Some" in
    forAll(
      Gen.zip(
        Arbitrary.arbitrary[String],
        Arbitrary.arbitrary[OnDoneToYieldNone[String, LocalDateTime, UUID]],
        Arbitrary.arbitrary[OnDoneToYieldSome[String, ZonedDateTime, UUID]]
      )) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[String, LocalDateTime, UUID]]("left stage")
        val rightStage = mock[Stage[String, ZonedDateTime, UUID]]("right stage")
        val leftYield = leftYieldSupplier(mock[OnDone[String, LocalDateTime, UUID]]("left onDone"))
        val rightYield = rightYieldSupplier(mock[OnDone[String, ZonedDateTime, UUID]]("right OnDone"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(Or(leftStage, rightStage)(in)) { case Yield.Some(out, signal, onDone) =>
          out shouldBe Right(rightYield.out)
          test(leftYield, rightYield, signal, onDone)
        }
    }

  private def test[I, LO, RO, E](
      leftYield: Yield[I, LO, E],
      rightYield: Yield[I, RO, E],
      signal: Signal[E],
      onDone: OnDone[I, Either[LO, RO], E]): Assertion = {
    signal shouldBe leftYield.signal ++ rightYield.signal

    val leftOnSuccessStage = mock[Stage[I, LO, E]]("left onSuccess stage")
    val rightOnSuccessStage = mock[Stage[I, RO, E]]("right onSuccess stage")
    inSequence {
      (leftYield.onDone.onSuccess _).expects().returns(leftOnSuccessStage)
      (rightYield.onDone.onSuccess _).expects().returns(rightOnSuccessStage)
    }
    onDone.onSuccess() shouldBe Or(leftOnSuccessStage, rightOnSuccessStage)

    val leftOnCompleteStage = mock[Stage[I, LO, E]]("left onComplete stage")
    val rightOnCompleteStage = mock[Stage[I, RO, E]]("right onComplete stage")
    inSequence {
      (leftYield.onDone.onComplete _).expects().returns(leftOnCompleteStage)
      (rightYield.onDone.onComplete _).expects().returns(rightOnCompleteStage)
    }
    onDone.onComplete() shouldBe Or(leftOnCompleteStage, rightOnCompleteStage)

    val leftOnErrorStage = mock[Stage[I, LO, E]]("left onError stage")
    val rightOnErrorStage = mock[Stage[I, RO, E]]("right onError stage")
    inSequence {
      (leftYield.onDone.onError _).expects().returns(leftOnErrorStage)
      (rightYield.onDone.onError _).expects().returns(rightOnErrorStage)
    }
    onDone.onError() shouldBe Or(leftOnErrorStage, rightOnErrorStage)
  }

  it should "return Left output if the left stage returns Yield.Some" in
    forAll(Gen.zip(Gen.uuid, Arbitrary.arbitrary[OnDoneToYieldSome[UUID, Long, String]])) {
      case (in, leftYieldSupplier) =>
        val leftStage = mock[Stage[UUID, Long, String]]("left stage")
        val rightStage = mock[Stage[UUID, ZoneId, String]]("right stage")
        val leftYield = leftYieldSupplier(mock[OnDone[UUID, Long, String]]("left onDone"))
        (leftStage.apply _).expects(in).returns(leftYield)
        inside(Or(leftStage, rightStage)(in)) { case Yield.Some(out, signal, onDone) =>
          out shouldBe Left(leftYield.out)
          signal shouldBe leftYield.signal
          testWrappedOnDone(onDone, leftYield.onDone, Or(_: Stage[UUID, Long, String], rightStage))
        }
    }
}
