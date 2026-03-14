package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

class EvolutionTest extends AnyFlatSpec with Matchers with MockFactory {
  "compose method" should "compose Evolution objects correctly" in {
    val previousEvolution = mock[Evolution[String, Instant, Exception]]
    val previousStage = mock[Stage[String, Instant, Exception]]
    val nextEvolution = mock[Evolution[Instant, Long, Exception]]
    val nextStage = mock[Stage[Instant, Long, Exception]]
    val evolution = previousEvolution.compose(nextEvolution)
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

  it should "compose Evolution and Stage objects correctly" in {
    val previousEvolution = mock[Evolution[String, Instant, Exception]]
    val previousStage = mock[Stage[String, Instant, Exception]]
    val nextStage = mock[Stage[Instant, Long, Exception]]
    val evolution = previousEvolution.compose(nextStage)
    val stage = previousStage ~> nextStage

    (previousEvolution.onSuccess _).expects().returns(previousStage)
    evolution.onSuccess() shouldBe stage

    (previousEvolution.onComplete _).expects().returns(previousStage)
    evolution.onComplete() shouldBe stage

    (previousEvolution.onError _).expects().returns(previousStage)
    evolution.onError() shouldBe stage
  }

  "map" should "transform stages correctly" in {
    val evolution = mock[Evolution[Long, Instant, UUID]]
    val f = mock[Stage[Long, Instant, UUID] => Stage[ZoneId, ZonedDateTime, String]]

    val onSuccessStage = mock[Stage[Long, Instant, UUID]]
    val onSuccessMappedStage = mock[Stage[ZoneId, ZonedDateTime, String]]
    (evolution.onSuccess _).expects().returns(onSuccessStage)
    (f.apply _).expects(onSuccessStage).returns(onSuccessMappedStage)
    evolution.map(f).onSuccess() shouldBe onSuccessMappedStage

    val onCompleteStage = mock[Stage[Long, Instant, UUID]]
    val onCompleteMappedStage = mock[Stage[ZoneId, ZonedDateTime, String]]
    (evolution.onComplete _).expects().returns(onCompleteStage)
    (f.apply _).expects(onCompleteStage).returns(onCompleteMappedStage)
    evolution.map(f).onComplete() shouldBe onCompleteMappedStage

    val onErrorStage = mock[Stage[Long, Instant, UUID]]
    val onErrorMappedStage = mock[Stage[ZoneId, ZonedDateTime, String]]
    (evolution.onError _).expects().returns(onErrorStage)
    (f.apply _).expects(onErrorStage).returns(onErrorMappedStage)
    evolution.map(f).onError() shouldBe onErrorMappedStage
  }

  "FromStage" should "return the same stage for any status" in {
    val stage = mock[Stage[Int, String, UUID]]
    val evolution = Evolution.FromStage(stage)
    evolution.onSuccess() should be theSameInstanceAs stage
    evolution.onComplete() should be theSameInstanceAs stage
    evolution.onError() should be theSameInstanceAs stage
  }
}
