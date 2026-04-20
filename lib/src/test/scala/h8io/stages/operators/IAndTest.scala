package h8io.stages.operators

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

class IAndTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "IAnd" should "return Yield.None if both stages return Yield.None" in
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
        inside(IAnd(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IAnd[Long, Duration, Instant, Exception])
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
        val rightYield = rightYieldSupplier(mock[Evolution[UUID, ZoneId, String]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(IAnd(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IAnd[UUID, Long, ZoneId, String])
        }
    }

  it should "return Yield.None if the left stage returns Yield.None and the right one returns Yield.Some" in
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
        inside(IAnd(leftStage, rightStage)(in)) { case Yield.None(status, evolution) =>
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IAnd[String, LocalDateTime, ZonedDateTime, UUID])
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
        val rightYield = rightYieldSupplier(mock[Evolution[ZoneOffset, LocalDate, Short]]("right evolution"))
        inSequence {
          (leftStage.apply _).expects(in).returns(leftYield)
          (rightStage.apply _).expects(in).returns(rightYield)
        }
        inside(IAnd(leftStage, rightStage)(in)) { case Yield.Some(out, status, evolution) =>
          out shouldBe leftYield.out -> rightYield.out
          status shouldBe leftYield.status.combine(rightYield.status)
          testEvolutionComposition(
            evolution, leftYield.evolution, rightYield.evolution, IAnd[ZoneOffset, OffsetDateTime, LocalDate, Short])
        }
    }

  it should "skip the left and right stages in sequence" in {
    val leftStage = mock[Stage[String, OffsetDateTime, Long]]("left stage")
    val leftEvolution = mock[Evolution[String, OffsetDateTime, Long]]("left evolution")
    val rightStage = mock[Stage[String, ZonedDateTime, Long]]("right stage")
    val rightEvolution = mock[Evolution[String, ZonedDateTime, Long]]("right evolution")
    inSequence {
      (leftStage.skip _).expects().returns(leftEvolution)
      (rightStage.skip _).expects().returns(rightEvolution)
    }
    val evolution = IAnd(leftStage, rightStage).skip()
    testEvolutionComposition(
      evolution, leftEvolution, rightEvolution, IAnd[String, OffsetDateTime, ZonedDateTime, Long])
  }
}
