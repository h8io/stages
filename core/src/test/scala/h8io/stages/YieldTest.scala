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

  "mapEvolution" should "transform Some content (status and evolution)" in
    forAll { (out: LocalDateTime, initialStatus: Status[Long], mappedStatus: Status[Exception]) =>
      val initialEvolution = mock[Evolution[Long, LocalDateTime, Long]]("initial Evolution")
      val mappedEvolution = mock[Evolution[String, LocalDateTime, Exception]]("mapped Evolution")
      val mapEvolution =
        mock[Evolution[Long, LocalDateTime, Long] => Evolution[String, LocalDateTime, Exception]]("mapEvolution")
      (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
      Yield.Some(out, initialStatus, initialEvolution).mapEvolution(mappedStatus, mapEvolution) shouldBe
        Yield.Some(out, mappedStatus, mappedEvolution)
    }

  it should "transform Some content (evolution)" in
    forAll { (out: LocalDateTime, status: Status[Long]) =>
      val initialEvolution = mock[Evolution[Long, LocalDateTime, Long]]("initial Evolution")
      val mappedEvolution = mock[Evolution[String, LocalDateTime, Long]]("mapped Evolution")
      val mapEvolution =
        mock[Evolution[Long, LocalDateTime, Long] => Evolution[String, LocalDateTime, Long]]("mapEvolution")
      (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
      Yield.Some(out, status, initialEvolution).mapEvolution(mapEvolution) shouldBe
        Yield.Some(out, status, mappedEvolution)
    }

  it should "transform None content (status and evolution)" in
    forAll { (initialStatus: Status[Exception], mappedStatus: Status[String]) =>
      val initialEvolution = mock[Evolution[ZonedDateTime, OffsetDateTime, Exception]]("initial Evolution")
      val mappedEvolution = mock[Evolution[Duration, OffsetDateTime, String]]("mapped Evolution")
      val mapEvolution =
        mock[Evolution[ZonedDateTime, OffsetDateTime, Exception] => Evolution[Duration, OffsetDateTime, String]](
          "mapEvolution")
      (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
      Yield.None(initialStatus, initialEvolution).mapEvolution(mappedStatus, mapEvolution) shouldBe
        Yield.None(mappedStatus, mappedEvolution)
    }

  it should "transform None content (evolution)" in
    forAll { (status: Status[Int]) =>
      val initialEvolution = mock[Evolution[ZonedDateTime, OffsetDateTime, Int]]("initial Evolution")
      val mappedEvolution = mock[Evolution[Duration, OffsetDateTime, Int]]("mapped Evolution")
      val mapEvolution =
        mock[Evolution[ZonedDateTime, OffsetDateTime, Int] => Evolution[Duration, OffsetDateTime, Int]]("mapEvolution")
      (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
      Yield.None(status, initialEvolution).mapEvolution(mapEvolution) shouldBe Yield.None(status, mappedEvolution)
    }

  "mapEvolutionAndBreak" should "transform Some content" in
    forAll { (out: LocalDateTime, status: Status[Long]) =>
      val initialEvolution = mock[Evolution[Long, LocalDateTime, Long]]("initial Evolution")
      val mappedEvolution = mock[Evolution[String, LocalDateTime, Long]]("mapped Evolution")
      val mapEvolution =
        mock[Evolution[Long, LocalDateTime, Long] => Evolution[String, LocalDateTime, Long]]("mapEvolution")
      (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
      Yield.Some(out, status, initialEvolution).mapEvolutionAndBreak(mapEvolution) shouldBe
        Yield.Some(out, status.break, mappedEvolution)
    }

  it should "transform None content" in
    forAll { (status: Status[Int]) =>
      val initialEvolution = mock[Evolution[ZonedDateTime, OffsetDateTime, Int]]("initial Evolution")
      val mappedEvolution = mock[Evolution[Duration, OffsetDateTime, Int]]("mapped Evolution")
      val mapEvolution =
        mock[Evolution[ZonedDateTime, OffsetDateTime, Int] => Evolution[Duration, OffsetDateTime, Int]]("mapEvolution")
      (mapEvolution.apply _).expects(initialEvolution).returns(mappedEvolution)
      Yield.None(status, initialEvolution).mapEvolutionAndBreak(mapEvolution) shouldBe
        Yield.None(status.break, mappedEvolution)
    }
}
