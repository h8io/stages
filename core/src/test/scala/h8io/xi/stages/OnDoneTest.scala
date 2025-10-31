package h8io.xi.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

class OnDoneTest extends AnyFlatSpec with Matchers with MockFactory {
  "compose method" should "compose OnDone objects correctly" in {
    val previousOnDone = mock[OnDone[String, Instant, Exception]]
    val previousStage = mock[Stage[String, Instant, Exception]]
    val nextOnDone = mock[OnDone[Instant, Long, Exception]]
    val nextStage = mock[Stage[Instant, Long, Exception]]
    val onDone = previousOnDone.compose(nextOnDone)
    val stage = previousStage ~> nextStage

    inSequence {
      (nextOnDone.onSuccess _).expects().returns(nextStage)
      (previousOnDone.onSuccess _).expects().returns(previousStage)
    }
    onDone.onSuccess() shouldBe stage

    inSequence {
      (nextOnDone.onComplete _).expects().returns(nextStage)
      (previousOnDone.onComplete _).expects().returns(previousStage)
    }
    onDone.onComplete() shouldBe stage

    inSequence {
      (nextOnDone.onError _).expects().returns(nextStage)
      (previousOnDone.onError _).expects().returns(previousStage)
    }
    onDone.onError() shouldBe stage
  }

  "map" should "transform stages correctly" in {
    val onDone = mock[OnDone[Long, Instant, UUID]]
    val f = mock[Stage[Long, Instant, UUID] => Stage[ZoneId, ZonedDateTime, String]]

    val onSuccessStage = mock[Stage[Long, Instant, UUID]]
    val onSuccessMappedStage = mock[Stage[ZoneId, ZonedDateTime, String]]
    (onDone.onSuccess _).expects().returns(onSuccessStage)
    (f.apply _).expects(onSuccessStage).returns(onSuccessMappedStage)
    onDone.map(f).onSuccess() shouldBe onSuccessMappedStage

    val onCompleteStage = mock[Stage[Long, Instant, UUID]]
    val onCompleteMappedStage = mock[Stage[ZoneId, ZonedDateTime, String]]
    (onDone.onComplete _).expects().returns(onCompleteStage)
    (f.apply _).expects(onCompleteStage).returns(onCompleteMappedStage)
    onDone.map(f).onComplete() shouldBe onCompleteMappedStage

    val onErrorStage = mock[Stage[Long, Instant, UUID]]
    val onErrorMappedStage = mock[Stage[ZoneId, ZonedDateTime, String]]
    (onDone.onError _).expects().returns(onErrorStage)
    (f.apply _).expects(onErrorStage).returns(onErrorMappedStage)
    onDone.map(f).onError() shouldBe onErrorMappedStage
  }
}
