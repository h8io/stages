package h8io.stages.cats

import cats.data.Ior
import h8io.stages.{Evolution, Stage, StagesCoreArbitraries, Status, Yield}
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.*
import java.util.UUID
import scala.concurrent.duration.Duration

class IOrTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries {
  "IOr" should "return Yield.None if both stages return Yield.None" in
    forAll(
      Gen.zip(Gen.long,
        Arbitrary.arbitrary[EvolutionToYieldNone[Long, Duration, Exception]],
        Arbitrary.arbitrary[EvolutionToYieldNone[Long, Instant, Exception]])) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[Long, Duration, Exception]]("left stage")
        val rightStage = mock[Stage[Long, Instant, Exception]]("right stage")
        val leftYield = leftYieldSupplier(mock[Evolution[Long, Duration, Exception]]("left evolution"))
        val rightYield = rightYieldSupplier(mock[Evolution[Long, Instant, Exception]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(IOr(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          test(leftYield, rightYield, status, evolution)
        }
    }

  it should "return Ior.Left output if the left stage returns Yield.Some" in
    forAll(
      Gen.zip(
        Gen.uuid,
        Arbitrary.arbitrary[EvolutionToYieldSome[UUID, Long, String]],
        Arbitrary.arbitrary[EvolutionToYieldNone[UUID, ZoneId, String]])) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[UUID, Long, String]]("left stage")
        val rightStage = mock[Stage[UUID, ZoneId, String]]("right stage")
        val leftYield = leftYieldSupplier(mock[Evolution[UUID, Long, String]]("left evolution"))
        val rightYield = rightYieldSupplier(mock[Evolution[UUID, ZoneId, String]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(IOr(leftStage, rightStage)(in)) { case Yield.Some(out, status, evolution) =>
          out shouldBe Ior.Left(leftYield.out)
          test(leftYield, rightYield, status, evolution)
        }
    }

  it should "return Ior.Right output if the right stage returns Yield.Some" in
    forAll(
      Gen.zip(
        Arbitrary.arbitrary[String],
        Arbitrary.arbitrary[EvolutionToYieldNone[String, LocalDateTime, UUID]],
        Arbitrary.arbitrary[EvolutionToYieldSome[String, ZonedDateTime, UUID]]
      )) {
      case (in, leftYieldSupplier, rightYieldSupplier) =>
        val leftStage = mock[Stage[String, LocalDateTime, UUID]]("left stage")
        val rightStage = mock[Stage[String, ZonedDateTime, UUID]]("right stage")
        val leftYield = leftYieldSupplier(mock[Evolution[String, LocalDateTime, UUID]]("left evolution"))
        val rightYield = rightYieldSupplier(mock[Evolution[String, ZonedDateTime, UUID]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(IOr(leftStage, rightStage)(in)) { case Yield.Some(out, status, evolution) =>
          out shouldBe Ior.Right(rightYield.out)
          test(leftYield, rightYield, status, evolution)
        }
    }

  it should "return Ior.Both output if both stages return Yield.Some" in
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
        val rightYield = rightYieldSupplier(mock[Evolution[ZoneOffset, LocalDate, Short]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(IOr(leftStage, rightStage)(in)) { case Yield.Some(out, status, evolution) =>
          out shouldBe Ior.Both(leftYield.out, rightYield.out)
          test(leftYield, rightYield, status, evolution)
        }
    }

  private def test[I, LO, RO, E](
      leftYield: Yield[I, LO, E],
      rightYield: Yield[I, RO, E],
      status: Status[E],
      evolution: Evolution[I, Ior[LO, RO], E]): Assertion = {
    status shouldBe leftYield.status ++ rightYield.status

    val leftOnSuccessStage = mock[Stage[I, LO, E]]("left onSuccess stage")
    val rightOnSuccessStage = mock[Stage[I, RO, E]]("right onSuccess stage")
    inSequence {
      (leftYield.evolution.onSuccess _).expects().returns(leftOnSuccessStage)
      (rightYield.evolution.onSuccess _).expects().returns(rightOnSuccessStage)
    }
    evolution.onSuccess() shouldBe IOr(leftOnSuccessStage, rightOnSuccessStage)

    val leftOnCompleteStage = mock[Stage[I, LO, E]]("left onComplete stage")
    val rightOnCompleteStage = mock[Stage[I, RO, E]]("right onComplete stage")
    inSequence {
      (leftYield.evolution.onComplete _).expects().returns(leftOnCompleteStage)
      (rightYield.evolution.onComplete _).expects().returns(rightOnCompleteStage)
    }
    evolution.onComplete() shouldBe IOr(leftOnCompleteStage, rightOnCompleteStage)

    val leftOnErrorStage = mock[Stage[I, LO, E]]("left onError stage")
    val rightOnErrorStage = mock[Stage[I, RO, E]]("right onError stage")
    inSequence {
      (leftYield.evolution.onError _).expects().returns(leftOnErrorStage)
      (rightYield.evolution.onError _).expects().returns(rightOnErrorStage)
    }
    evolution.onError() shouldBe IOr(leftOnErrorStage, rightOnErrorStage)
  }

  "Left" should "return Yield.Some if the input is cats.data.Ior.Left" in {
    val value = mock[AnyRef]
    IOr.Left(cats.data.Ior.Left(value)) shouldBe Yield.Some(value, Status.Success, IOr.Left)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Both" in {
    val value = mock[AnyRef]
    IOr.Left(cats.data.Ior.Both(value, mock[AnyRef])) shouldBe Yield.Some(value, Status.Success, IOr.Left)
  }

  it should "return Yield.None if the input is cats.data.Ior.Right" in {
    IOr.Left[AnyRef].apply(cats.data.Ior.Right(mock[AnyRef])) shouldBe Yield.None(Status.Success, IOr.Left)
  }

  "Right" should "return Yield.None if the input is cats.data.Ior.Left" in {
    IOr.Right[AnyRef].apply(cats.data.Ior.Left(mock[AnyRef])) shouldBe Yield.None(Status.Success, IOr.Right)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Both" in {
    val value = mock[AnyRef]
    IOr.Right(cats.data.Ior.Both(mock[AnyRef], value)) shouldBe Yield.Some(value, Status.Success, IOr.Right)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Right" in {
    val value = mock[AnyRef]
    IOr.Right(cats.data.Ior.Right(value)) shouldBe Yield.Some(value, Status.Success, IOr.Right)
  }
}
