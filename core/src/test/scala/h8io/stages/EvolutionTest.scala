package h8io.stages

import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID

class EvolutionTest extends AnyFlatSpec with Matchers with MockFactory with StagesCoreTestUtil {
  "compose method" should "compose Evolution objects with Evolution.AndThen" in {
    val upstreamEvolution = mock[Evolution[Instant, Long, Exception]]
    val downstreamEvolution = mock[Evolution[String, Instant, Exception]]
    downstreamEvolution.compose(upstreamEvolution) shouldBe Evolution.AndThen(upstreamEvolution, downstreamEvolution)
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

    (evolution.dispose _).expects()
    noException should be thrownBy evolution.map(f).dispose()
  }

  "AndThen" should "compose Evolution objects correctly" in {
    val upstreamEvolution = mock[Evolution[Instant, Long, Exception]]
    val downstreamEvolution = mock[Evolution[String, Instant, Exception]]
    testEvolutionComposition(
      Evolution.AndThen(upstreamEvolution, downstreamEvolution),
      downstreamEvolution,
      upstreamEvolution,
      Stage.AndThen[String, Instant, Long, Exception])
  }

  it should "call downstream dispose even when upstream dispose throws" in {
    val upstreamEvolution = mock[Evolution[Instant, Long, Exception]]
    val downstreamEvolution = mock[Evolution[String, Instant, Exception]]
    val upstreamException = new Exception("upstream")
    inSequence {
      (upstreamEvolution.dispose _).expects().throws(upstreamException)
      (downstreamEvolution.dispose _).expects()
    }
    the[Exception] thrownBy Evolution.AndThen(upstreamEvolution, downstreamEvolution).dispose() shouldBe
      upstreamException
  }

  it should "propagate downstream dispose exception" in {
    val upstreamEvolution = mock[Evolution[Instant, Long, Exception]]
    val downstreamEvolution = mock[Evolution[String, Instant, Exception]]
    val downstreamException = new Exception("downstream")
    inSequence {
      (upstreamEvolution.dispose _).expects()
      (downstreamEvolution.dispose _).expects().throws(downstreamException)
    }
    the[Exception] thrownBy Evolution.AndThen(upstreamEvolution, downstreamEvolution).dispose() shouldBe
      downstreamException
  }

  it should "suppress downstream exception when both disposes throw" in {
    val upstreamEvolution = mock[Evolution[Instant, Long, Exception]]
    val downstreamEvolution = mock[Evolution[String, Instant, Exception]]
    val upstreamException = new Exception("upstream")
    val downstreamException = new Exception("downstream")
    inSequence {
      (upstreamEvolution.dispose _).expects().throws(upstreamException)
      (downstreamEvolution.dispose _).expects().throws(downstreamException)
    }
    val thrown = the[Exception] thrownBy Evolution.AndThen(upstreamEvolution, downstreamEvolution).dispose()
    thrown shouldBe upstreamException
    thrown.getSuppressed should contain only downstreamException
  }
}
