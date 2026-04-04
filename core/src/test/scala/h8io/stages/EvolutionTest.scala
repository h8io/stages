package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

class EvolutionTest extends AnyFlatSpec with Matchers with MockFactory {
  "compose method" should "compose Evolution objects correctly" in {
    val upstreamEvolution = mock[Evolution[String, Instant, Exception]]
    val upstreamStage = mock[Stage[String, Instant, Exception]]
    val downstreamEvolution = mock[Evolution[Instant, Long, Exception]]
    val downstreamStage = mock[Stage[Instant, Long, Exception]]
    val evolution = upstreamEvolution.compose(downstreamEvolution)
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

  it should "compose Evolution and Stage objects correctly" in {
    val upstreamEvolution = mock[Evolution[String, Instant, Exception]]
    val upstreamStage = mock[Stage[String, Instant, Exception]]
    val downstreamStage = mock[Stage[Instant, Long, Exception]]
    val evolution = upstreamEvolution.compose(downstreamStage)
    val stage = upstreamStage ~> downstreamStage

    (upstreamEvolution.onSuccess _).expects().returns(upstreamStage)
    evolution.onSuccess() shouldBe stage

    (upstreamEvolution.onComplete _).expects().returns(upstreamStage)
    evolution.onComplete() shouldBe stage

    (upstreamEvolution.onError _).expects().returns(upstreamStage)
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
}
