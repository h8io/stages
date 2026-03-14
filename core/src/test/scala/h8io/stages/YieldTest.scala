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
      (previousYieldSupplier: OnDoneToYieldSome[Long, Instant, String],
          nextYieldSupplier: OnDoneToYieldSome[Instant, String, String]) =>
        val previousOnDone = mock[OnDone[Long, Instant, String]]
        val previousYield = previousYieldSupplier(previousOnDone)
        val nextOnDone = mock[OnDone[Instant, String, String]]
        val nextYield = nextYieldSupplier(nextOnDone)
        inside(previousYield.compose(nextYield)) {
          case Yield.Some(nextYield.out, status, onDone) =>
            status shouldBe previousYield.status ++ nextYield.status
            val previousStage = mock[Stage[Long, Instant, String]]
            val nextStage = mock[Stage[Instant, String, String]]
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
    }

  it should "compose Some and None correctly" in
    forAll {
      (previousYieldSupplier: OnDoneToYieldSome[Long, Instant, String],
          nextYieldSupplier: OnDoneToYieldNone[Instant, String, String]) =>
        val previousOnDone = mock[OnDone[Long, Instant, String]]
        val previousYield = previousYieldSupplier(previousOnDone)
        val nextOnDone = mock[OnDone[Instant, String, String]]
        val nextYield = nextYieldSupplier(nextOnDone)
        inside(previousYield.compose(nextYield)) {
          case Yield.None(status, onDone) =>
            status shouldBe previousYield.status ++ nextYield.status
            val previousStage = mock[Stage[Long, Instant, String]]
            val nextStage = mock[Stage[Instant, String, String]]
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
    }

  it should "compose None and Stage correctly" in
    forAll { (previousYieldSupplier: OnDoneToYieldNone[Long, Instant, String]) =>
      val previousOnDone = mock[OnDone[Long, Instant, String]]("previous onDone")
      val nextStage = mock[Stage[Instant, String, String]]("next stage")
      val previousYield = previousYieldSupplier(previousOnDone)
      inside(previousYield.compose(nextStage)) {
        case Yield.None(previousYield.`status`, onDone) =>
          val onSuccessStage = mock[Stage[Long, Instant, String]]("onSuccess stage")
          (previousOnDone.onSuccess _).expects().returns(onSuccessStage)
          onDone.onSuccess() shouldBe onSuccessStage ~> nextStage

          val onCompleteStage = mock[Stage[Long, Instant, String]]("onComplete stage")
          (previousOnDone.onComplete _).expects().returns(onCompleteStage)
          onDone.onComplete() shouldBe onCompleteStage ~> nextStage

          val onErrorStage = mock[Stage[Long, Instant, String]]("onError stage")
          (previousOnDone.onError _).expects().returns(onErrorStage)
          onDone.onError() shouldBe onErrorStage ~> nextStage
      }
    }

  "mapOnDone" should "transform Some content (status and onDone)" in
    forAll { (out: LocalDateTime, initialStatus: Status[Long], mappedStatus: Status[Exception]) =>
      val initialOnDone = mock[OnDone[Long, LocalDateTime, Long]]("initial OnDone")
      val mappedOnDone = mock[OnDone[String, LocalDateTime, Exception]]("mapped OnDone")
      val mapOnDone =
        mock[OnDone[Long, LocalDateTime, Long] => OnDone[String, LocalDateTime, Exception]]("mapOnDone")
      (mapOnDone.apply _).expects(initialOnDone).returns(mappedOnDone)
      Yield.Some(out, initialStatus, initialOnDone).mapOnDone(mappedStatus, mapOnDone) shouldBe
        Yield.Some(out, mappedStatus, mappedOnDone)
    }

  it should "transform Some content (onDone)" in
    forAll { (out: LocalDateTime, status: Status[Long]) =>
      val initialOnDone = mock[OnDone[Long, LocalDateTime, Long]]("initial OnDone")
      val mappedOnDone = mock[OnDone[String, LocalDateTime, Long]]("mapped OnDone")
      val mapOnDone =
        mock[OnDone[Long, LocalDateTime, Long] => OnDone[String, LocalDateTime, Long]]("mapOnDone")
      (mapOnDone.apply _).expects(initialOnDone).returns(mappedOnDone)
      Yield.Some(out, status, initialOnDone).mapOnDone(mapOnDone) shouldBe Yield.Some(out, status, mappedOnDone)
    }

  it should "transform None content (status and onDone)" in
    forAll { (initialStatus: Status[Exception], mappedStatus: Status[String]) =>
      val initialOnDone = mock[OnDone[ZonedDateTime, OffsetDateTime, Exception]]("initial OnDone")
      val mappedOnDone = mock[OnDone[Duration, OffsetDateTime, String]]("mapped OnDone")
      val mapOnDone =
        mock[OnDone[ZonedDateTime, OffsetDateTime, Exception] => OnDone[Duration, OffsetDateTime, String]]("mapOnDone")
      (mapOnDone.apply _).expects(initialOnDone).returns(mappedOnDone)
      Yield.None(initialStatus, initialOnDone).mapOnDone(mappedStatus, mapOnDone) shouldBe
        Yield.None(mappedStatus, mappedOnDone)
    }

  it should "transform None content (onDone)" in
    forAll { (status: Status[Int]) =>
      val initialOnDone = mock[OnDone[ZonedDateTime, OffsetDateTime, Int]]("initial OnDone")
      val mappedOnDone = mock[OnDone[Duration, OffsetDateTime, Int]]("mapped OnDone")
      val mapOnDone =
        mock[OnDone[ZonedDateTime, OffsetDateTime, Int] => OnDone[Duration, OffsetDateTime, Int]]("mapOnDone")
      (mapOnDone.apply _).expects(initialOnDone).returns(mappedOnDone)
      Yield.None(status, initialOnDone).mapOnDone(mapOnDone) shouldBe Yield.None(status, mappedOnDone)
    }

  "mapOnDoneAndBreak" should "transform Some content" in
    forAll { (out: LocalDateTime, status: Status[Long]) =>
      val initialOnDone = mock[OnDone[Long, LocalDateTime, Long]]("initial OnDone")
      val mappedOnDone = mock[OnDone[String, LocalDateTime, Long]]("mapped OnDone")
      val mapOnDone =
        mock[OnDone[Long, LocalDateTime, Long] => OnDone[String, LocalDateTime, Long]]("mapOnDone")
      (mapOnDone.apply _).expects(initialOnDone).returns(mappedOnDone)
      Yield.Some(out, status, initialOnDone).mapOnDoneAndBreak(mapOnDone) shouldBe
        Yield.Some(out, status.break, mappedOnDone)
    }

  it should "transform None content" in
    forAll { (status: Status[Int]) =>
      val initialOnDone = mock[OnDone[ZonedDateTime, OffsetDateTime, Int]]("initial OnDone")
      val mappedOnDone = mock[OnDone[Duration, OffsetDateTime, Int]]("mapped OnDone")
      val mapOnDone =
        mock[OnDone[ZonedDateTime, OffsetDateTime, Int] => OnDone[Duration, OffsetDateTime, Int]]("mapOnDone")
      (mapOnDone.apply _).expects(initialOnDone).returns(mappedOnDone)
      Yield.None(status, initialOnDone).mapOnDoneAndBreak(mapOnDone) shouldBe Yield.None(status.break, mappedOnDone)
    }
}
