package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.*

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
      (previousYieldSupplier: EvolutionToYieldSome[Long, Instant, String],
          nextYieldSupplier: EvolutionToYieldSome[Instant, String, String]) =>
        val previousEvolution = mock[Evolution[Long, Instant, String]]
        val previousYield = previousYieldSupplier(previousEvolution)
        val nextEvolution = mock[Evolution[Instant, String, String]]
        val nextYield = nextYieldSupplier(nextEvolution)
        inside(previousYield.compose(nextYield)) {
          case Yield.Some(nextYield.out, status, evolution) =>
            status shouldBe previousYield.status ++ nextYield.status
            val previousStage = mock[Stage[Long, Instant, String]]
            val nextStage = mock[Stage[Instant, String, String]]
            val stage = previousStage ~> nextStage

            inSequence {
              (nextEvolution.onSuccess _).expects().returns(nextStage)
              (previousEvolution.onSuccess _).expects().returns(previousStage)
            }
            evolution.onSuccess() shouldBe stage

            inSequence {
              (nextEvolution.onComplete _).expects().returns(nextStage)
              (previousEvolution.onComplete _).expects().returns(previousStage)
            }
            evolution.onComplete() shouldBe stage

            inSequence {
              (nextEvolution.onError _).expects().returns(nextStage)
              (previousEvolution.onError _).expects().returns(previousStage)
            }
            evolution.onError() shouldBe stage
        }
    }

  it should "compose Some and None correctly" in
    forAll {
      (previousYieldSupplier: EvolutionToYieldSome[Long, Instant, String],
          nextYieldSupplier: EvolutionToYieldNone[Instant, String, String]) =>
        val previousEvolution = mock[Evolution[Long, Instant, String]]
        val previousYield = previousYieldSupplier(previousEvolution)
        val nextEvolution = mock[Evolution[Instant, String, String]]
        val nextYield = nextYieldSupplier(nextEvolution)
        inside(previousYield.compose(nextYield)) {
          case Yield.None(status, evolution) =>
            status shouldBe previousYield.status ++ nextYield.status
            val previousStage = mock[Stage[Long, Instant, String]]
            val nextStage = mock[Stage[Instant, String, String]]
            val stage = previousStage ~> nextStage

            inSequence {
              (nextEvolution.onSuccess _).expects().returns(nextStage)
              (previousEvolution.onSuccess _).expects().returns(previousStage)
            }
            evolution.onSuccess() shouldBe stage

            inSequence {
              (nextEvolution.onComplete _).expects().returns(nextStage)
              (previousEvolution.onComplete _).expects().returns(previousStage)
            }
            evolution.onComplete() shouldBe stage

            inSequence {
              (nextEvolution.onError _).expects().returns(nextStage)
              (previousEvolution.onError _).expects().returns(previousStage)
            }
            evolution.onError() shouldBe stage
        }
    }

  it should "compose None and Stage correctly" in
    forAll { (previousYieldSupplier: EvolutionToYieldNone[Long, Instant, String]) =>
      val previousEvolution = mock[Evolution[Long, Instant, String]]("previous evolution")
      val nextStage = mock[Stage[Instant, String, String]]("next stage")
      val previousYield = previousYieldSupplier(previousEvolution)
      inside(previousYield.compose(nextStage)) {
        case Yield.None(previousYield.`status`, evolution) =>
          val onSuccessStage = mock[Stage[Long, Instant, String]]("onSuccess stage")
          (previousEvolution.onSuccess _).expects().returns(onSuccessStage)
          evolution.onSuccess() shouldBe onSuccessStage ~> nextStage

          val onCompleteStage = mock[Stage[Long, Instant, String]]("onComplete stage")
          (previousEvolution.onComplete _).expects().returns(onCompleteStage)
          evolution.onComplete() shouldBe onCompleteStage ~> nextStage

          val onErrorStage = mock[Stage[Long, Instant, String]]("onError stage")
          (previousEvolution.onError _).expects().returns(onErrorStage)
          evolution.onError() shouldBe onErrorStage ~> nextStage
      }
    }
}
