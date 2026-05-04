package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.StageOps
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.Duration

class SafeTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "Safe" should "wrap status and evolution for Yield.Some with a non-error status" in
    forAll(
      Gen.zip(
        Gen.long,
        arbStatusAndEvolutionToYieldSome[Long, Instant, UUID].arbitrary,
        Gen.oneOf(Status.Success, Status.complete))) {
      case (in, yieldSupplier, status) =>
        val yld = yieldSupplier(status, mock[Evolution[Long, Instant, UUID]])
        val stage = mock[Stage[Long, Instant, UUID]]
        (stage.apply _).expects(in).returns(yld)
        inside(Safe(stage)(in)) { case Yield.Some(yld.out, `status`, safeEvolution) =>
          testMappedEvolution(safeEvolution, yld.evolution, Safe[Long, Instant, UUID])
        }
    }

  it should "wrap status and evolution for Yield.None with a non-error status" in
    forAll(Gen.zip(Gen.uuid, Gen.oneOf(Status.Success, Status.complete))) {
      case (in, status) =>
        val yld = Yield.None(status, mock[Evolution[UUID, Long, String]])
        val stage = mock[Stage[UUID, Long, String]]
        (stage.apply _).expects(in).returns(yld)
        inside(Safe(stage)(in)) { case Yield.None(`status`, safeEvolution) =>
          testMappedEvolution(safeEvolution, yld.evolution, Safe[UUID, Long, String])
        }
    }

  it should "wrap status and evolution for Yield.Some with an error status" in
    forAll(
      Gen.zip(
        Gen.long,
        arbStatusAndEvolutionToYieldSome[Long, Instant, UUID].arbitrary,
        Gen.nonEmptyListOf(Gen.uuid))) {
      case (in, yieldSupplier, errors) =>
        val yld = yieldSupplier(Status.Complete(errors), mock[Evolution[Long, Instant, UUID]])
        val stage = mock[Stage[Long, Instant, UUID]]
        (stage.apply _).expects(in).returns(yld)
        val expectedErrors = errors.map(Right[Throwable, UUID])
        val expectedStatus = Status.Complete(expectedErrors)
        inside(Safe(stage)(in)) { case Yield.Some(yld.out, `expectedStatus`, safeEvolution) =>
          testMappedEvolution(safeEvolution, yld.evolution, Safe[Long, Instant, UUID])
        }
    }

  it should "wrap status and evolution for Yield.None with an error status" in
    forAll(Gen.zip(Gen.uuid, Gen.nonEmptyListOf(Gen.alphaNumStr))) {
      case (in, errors) =>
        val yld = Yield.None(Status.Complete(errors), mock[Evolution[UUID, Long, String]])
        val stage = mock[Stage[UUID, Long, String]]
        (stage.apply _).expects(in).returns(yld)
        val expectedErrors = errors.map(Right[Throwable, String])
        val expectedStatus = Status.Complete(expectedErrors)
        inside(Safe(stage)(in)) { case Yield.None(`expectedStatus`, safeEvolution) =>
          testMappedEvolution(safeEvolution, yld.evolution, Safe[UUID, Long, String])
        }
    }

  it should "recover from NonFatal exceptions thrown by alterand" in {
    val e = new RuntimeException
    val stage = mock[Stage[String, Duration, UUID]]
    (stage.apply _).expects("continuum").throws(e)
    val expectedStatus = Status.error(Left(e))
    val expectedEvolution = Safe(stage).toEvolution
    Safe(stage)("continuum") shouldBe Yield.None(`expectedStatus`, `expectedEvolution`)
  }

  it should "call the alterand.skip() method" in {
    val stage = mock[Stage[UUID, String, Exception]]("alterand")
    val evolution = mock[Evolution[UUID, String, Exception]]("evolution")
    (stage.skip _).expects().returns(evolution)
    testMappedEvolution(Safe(stage).skip(), evolution, Safe[UUID, String, Exception])
  }
}
