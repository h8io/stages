package h8io.stages.cats

import cats.data.Ior
import h8io.stages.*
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
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
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
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
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IOr[Long, Duration, Instant, Exception])
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
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IOr[UUID, Long, ZoneId, String])
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
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IOr[String, LocalDateTime, ZonedDateTime, UUID])
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
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IOr[ZoneOffset, OffsetDateTime, LocalDate, Short])
        }
    }

  it should "skip the left and the right stages in sequence" in {
    val leftStage = mock[Stage[String, OffsetDateTime, Long]]("left stage")
    val leftEvolution = mock[Evolution[String, OffsetDateTime, Long]]("left evolution")
    val rightStage = mock[Stage[String, ZonedDateTime, Long]]("right stage")
    val rightEvolution = mock[Evolution[String, ZonedDateTime, Long]]("right evolution")
    inSequence {
      (leftStage.skip _).expects().returns(leftEvolution)
      (rightStage.skip _).expects().returns(rightEvolution)
    }
    val evolution = IOr(leftStage, rightStage).skip()
    testEvolutionComposition(
      evolution, leftEvolution, rightEvolution, IOr.apply[String, OffsetDateTime, ZonedDateTime, Long])
  }

  "Left" should "return Yield.Some if the input is cats.data.Ior.Left" in {
    val value = mock[AnyRef]
    IOr.Left(Ior.Left(value)) shouldBe Yield.Some(value, Status.Success, IOr.Left)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Both" in {
    val value = mock[AnyRef]
    IOr.Left(Ior.Both(value, mock[AnyRef])) shouldBe Yield.Some(value, Status.Success, IOr.Left)
  }

  it should "return Yield.None if the input is cats.data.Ior.Right" in {
    IOr.Left[AnyRef].apply(Ior.Right(mock[AnyRef])) shouldBe Yield.None(Status.Success, IOr.Left)
  }

  "Right" should "return Yield.None if the input is cats.data.Ior.Left" in {
    IOr.Right[AnyRef].apply(Ior.Left(mock[AnyRef])) shouldBe Yield.None(Status.Success, IOr.Right)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Both" in {
    val value = mock[AnyRef]
    IOr.Right(Ior.Both(mock[AnyRef], value)) shouldBe Yield.Some(value, Status.Success, IOr.Right)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Right" in {
    val value = mock[AnyRef]
    IOr.Right(Ior.Right(value)) shouldBe Yield.Some(value, Status.Success, IOr.Right)
  }
}
