package h8io.stages

import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.*
import java.util.UUID

class YieldTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil {
  "compose method" should "compose Some and Some correctly" in
    forAll {
      (upstreamYieldSupplier: EvolutionToYieldSome[Long, Instant, String],
          downstreamYieldSupplier: EvolutionToYieldSome[Instant, String, String]) =>
        val upstreamEvolution = mock[Evolution[Long, Instant, String]]
        val upstreamYield = upstreamYieldSupplier(upstreamEvolution)
        val downstreamEvolution = mock[Evolution[Instant, String, String]]
        val downstreamYield = downstreamYieldSupplier(downstreamEvolution)
        inside(upstreamYield.compose(downstreamYield)) {
          case Yield.Some(downstreamYield.out, status, evolution) =>
            status shouldBe upstreamYield.status ++ downstreamYield.status
            val upstreamStage = mock[Stage[Long, Instant, String]]
            val downstreamStage = mock[Stage[Instant, String, String]]
            val stage = upstreamStage ~> downstreamStage

            inSequence {
              (downstreamEvolution.onSuccess _).expects().returns(downstreamStage)
              (upstreamEvolution.onSuccess _).expects().returns(upstreamStage)
            }
            evolution.onSuccess() shouldBe stage

            inSequence {
              (downstreamEvolution.onComplete _).expects().returns(downstreamStage)
              (upstreamEvolution.onComplete _).expects().returns(upstreamStage)
            }
            evolution.onComplete() shouldBe stage

            inSequence {
              (downstreamEvolution.onError _).expects().returns(downstreamStage)
              (upstreamEvolution.onError _).expects().returns(upstreamStage)
            }
            evolution.onError() shouldBe stage
        }
    }

  it should "compose Some and None correctly" in
    forAll {
      (upstreamYieldSupplier: EvolutionToYieldSome[Long, Instant, String],
          downstreamYieldSupplier: EvolutionToYieldNone[Instant, String, String]) =>
        val upstreamEvolution = mock[Evolution[Long, Instant, String]]
        val upstreamYield = upstreamYieldSupplier(upstreamEvolution)
        val downstreamEvolution = mock[Evolution[Instant, String, String]]
        val downstreamYield = downstreamYieldSupplier(downstreamEvolution)
        inside(upstreamYield.compose(downstreamYield)) {
          case Yield.None(status, evolution) =>
            status shouldBe upstreamYield.status ++ downstreamYield.status
            val upstreamStage = mock[Stage[Long, Instant, String]]
            val downstreamStage = mock[Stage[Instant, String, String]]
            val stage = upstreamStage ~> downstreamStage

            inSequence {
              (downstreamEvolution.onSuccess _).expects().returns(downstreamStage)
              (upstreamEvolution.onSuccess _).expects().returns(upstreamStage)
            }
            evolution.onSuccess() shouldBe stage

            inSequence {
              (downstreamEvolution.onComplete _).expects().returns(downstreamStage)
              (upstreamEvolution.onComplete _).expects().returns(upstreamStage)
            }
            evolution.onComplete() shouldBe stage

            inSequence {
              (downstreamEvolution.onError _).expects().returns(downstreamStage)
              (upstreamEvolution.onError _).expects().returns(upstreamStage)
            }
            evolution.onError() shouldBe stage
        }
    }

  it should "compose None and Evolution correctly" in
    forAll { (upstreamYieldSupplier: EvolutionToYieldNone[Long, Instant, String]) =>
      val upstreamEvolution = mock[Evolution[Long, Instant, String]]("upstream evolution")
      val downstreamEvolution = mock[Evolution[Instant, String, String]]("downstream evolution")
      val upstreamYield = upstreamYieldSupplier(upstreamEvolution)
      inside(upstreamYield.compose(downstreamEvolution)) {
        case Yield.None(upstreamYield.`status`, evolution) =>
          val onSuccessDownstreamStage = mock[Stage[Instant, String, String]]("onSuccess downstream stage")
          (downstreamEvolution.onSuccess _).expects().returns(onSuccessDownstreamStage)
          val onSuccessUpstreamStage = mock[Stage[Long, Instant, String]]("onSuccess upstream stage")
          (upstreamEvolution.onSuccess _).expects().returns(onSuccessUpstreamStage)
          evolution.onSuccess() shouldBe onSuccessUpstreamStage ~> onSuccessDownstreamStage

          val onCompleteDownstreamStage = mock[Stage[Instant, String, String]]("onComplete downstream stage")
          (downstreamEvolution.onComplete _).expects().returns(onCompleteDownstreamStage)
          val onCompleteUpstreamStage = mock[Stage[Long, Instant, String]]("onComplete upstream stage")
          (upstreamEvolution.onComplete _).expects().returns(onCompleteUpstreamStage)
          evolution.onComplete() shouldBe onCompleteUpstreamStage ~> onCompleteDownstreamStage

          val onErrorDownstreamStage = mock[Stage[Instant, String, String]]("onError downstream stage")
          (downstreamEvolution.onError _).expects().returns(onErrorDownstreamStage)
          val onErrorUpstreamStage = mock[Stage[Long, Instant, String]]("onError upstream stage")
          (upstreamEvolution.onError _).expects().returns(onErrorUpstreamStage)
          evolution.onError() shouldBe onErrorUpstreamStage ~> onErrorDownstreamStage
      }
    }

  "map" should "transform Yield.Some correctly" in
    forAll(Gen.zip(Gen.uuid, Gen.long, arbStatus[Long].arbitrary, arbStatus[String].arbitrary)) {
      case (initialOut, mappedOut, initialStatus, mappedStatus) =>
        val mapOut = mock[UUID => Long]
        (mapOut.apply _).expects(initialOut).returns(mappedOut)
        val mapStatus = mock[Status[Long] => Status[String]]
        (mapStatus.apply _).expects(initialStatus).returns(mappedStatus)
        val initialEvolution = mock[Evolution[Long, UUID, Long]]("initial Evolution")
        val mappedEvolution = mock[Evolution[String, Long, String]]("mapped Evolution")
        val mapEvolution =
          mock[Evolution[Long, UUID, Long] => Evolution[String, Long, String]]("mapEvolution")
        (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
        Yield.Some(initialOut, initialStatus, initialEvolution).map(mapOut, mapStatus, mapEvolution) shouldBe
          Yield.Some(mappedOut, mappedStatus, mappedEvolution)
    }

  it should "transform Yield.None correctly without mapping output" in
    forAll(Gen.zip(arbStatus[Throwable].arbitrary, arbStatus[Long].arbitrary)) {
      case (initialStatus, mappedStatus) =>
        val mapOut = mock[LocalDateTime => UUID]
        val mapStatus = mock[Status[Throwable] => Status[Long]]
        (mapStatus.apply _).expects(initialStatus).returns(mappedStatus)
        val initialEvolution = mock[Evolution[Instant, LocalDateTime, Throwable]]("initial Evolution")
        val mappedEvolution = mock[Evolution[String, UUID, Long]]("mapped Evolution")
        val mapEvolution =
          mock[Evolution[Instant, LocalDateTime, Throwable] => Evolution[String, UUID, Long]]("mapEvolution")
        (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
        Yield.None(initialStatus, initialEvolution).map(mapOut, mapStatus, mapEvolution) shouldBe
          Yield.None(mappedStatus, mappedEvolution)
    }
}
