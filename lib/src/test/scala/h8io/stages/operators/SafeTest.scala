package h8io.stages.operators

import h8io.stages.base.StagesBaseTestUtil
import h8io.stages.*
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
    with StagesBaseTestUtil {
  "Safe" should "wrap status and evolution for Yield.Some with a non-error status" in
    forAll(
      Gen.zip(
        Gen.long,
        arbStatusAndEvolutionToYieldSome[Long, Instant, UUID].arbitrary,
        Gen.oneOf(Status.Success, Status.Complete))) {
      case (in, yieldSupplier, status) =>
        val yld = yieldSupplier(status, mock[Evolution[Long, Instant, UUID]])
        val stage = mock[Stage[Long, Instant, UUID]]
        (stage.apply _).expects(in).returns(yld)
        inside(Safe(stage)(in)) { case Yield.Some(yld.out, `status`, safeEvolution) =>
          testWrappedEvolution(safeEvolution, yld.evolution, Safe[Long, Instant, UUID])
        }
    }

  it should "wrap status and evolution for Yield.None with a non-error status" in
    forAll(Gen.zip(Gen.uuid, Gen.oneOf(Status.Success, Status.Complete))) {
      case (in, status) =>
        val yld = Yield.None(status, mock[Evolution[UUID, Long, String]])
        val stage = mock[Stage[UUID, Long, String]]
        (stage.apply _).expects(in).returns(yld)
        inside(Safe(stage)(in)) { case Yield.None(`status`, safeEvolution) =>
          testWrappedEvolution(safeEvolution, yld.evolution, Safe[UUID, Long, String])
        }
    }

  it should "wrap status and evolution for Yield.Some with an error status" in
    forAll(
      Gen.zip(
        Gen.long,
        arbStatusAndEvolutionToYieldSome[Long, Instant, UUID].arbitrary,
        Gen.nonEmptyListOf(Gen.uuid))) {
      case (in, yieldSupplier, errors) =>
        val yld = yieldSupplier(Status.Error(errors.head, errors.tail), mock[Evolution[Long, Instant, UUID]])
        val stage = mock[Stage[Long, Instant, UUID]]
        (stage.apply _).expects(in).returns(yld)
        val expectedErrors = errors.map(Right[Throwable, UUID])
        val expectedStatus = Status.Error(expectedErrors.head, expectedErrors.tail)
        inside(Safe(stage)(in)) { case Yield.Some(yld.out, `expectedStatus`, safeEvolution) =>
          testWrappedEvolution(safeEvolution, yld.evolution, Safe[Long, Instant, UUID])
        }
    }

  it should "wrap status and evolution for Yield.None with an error status" in
    forAll(Gen.zip(Gen.uuid, Gen.nonEmptyListOf(Gen.alphaNumStr))) {
      case (in, errors) =>
        val yld = Yield.None(Status.Error(errors.head, errors.tail), mock[Evolution[UUID, Long, String]])
        val stage = mock[Stage[UUID, Long, String]]
        (stage.apply _).expects(in).returns(yld)
        val expectedErrors = errors.map(Right[Throwable, String])
        val expectedStatus = Status.Error(expectedErrors.head, expectedErrors.tail)
        inside(Safe(stage)(in)) { case Yield.None(`expectedStatus`, safeEvolution) =>
          testWrappedEvolution(safeEvolution, yld.evolution, Safe[UUID, Long, String])
        }
    }

  it should "recover from NonFatal exceptions thrown by alterand" in {
    val e = new RuntimeException
    val stage = mock[Stage[String, Duration, UUID]]
    (stage.apply _).expects("continuum").throws(e)
    val expectedStatus = Status.Error(Left(e))
    inside(Safe(stage)("continuum")) { case Yield.None(`expectedStatus`, safeEvolution) =>
      safeEvolution.onSuccess() shouldBe Safe(stage)
      safeEvolution.onComplete() shouldBe Safe(stage)
      safeEvolution.onError() shouldBe Safe(stage)
    }
  }

  it should "dispose the alterand" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]
    (alterand.dispose _).expects()
    noException should be thrownBy Safe(alterand).dispose()
  }
}
