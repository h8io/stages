package h8io.stages.operators

import h8io.stages.*
import h8io.stages.base.StagesBaseTestUtil
import h8io.stages.std.DeadEnd
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.{Duration as jDuration, Instant, ZoneId, ZonedDateTime}
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class LocalSoftDeadlineTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries
    with StagesCoreTestUtil
    with StagesBaseTestUtil {
  "LocalSoftDeadline" should "return DeadEnd if Scala duration is not positive" in
    forAll(Gen.choose(Long.MinValue, 0L)) { nanos =>
      val stage = mock[Stage[Any, Nothing, Nothing]]
      LocalSoftDeadline(FiniteDuration(nanos, TimeUnit.NANOSECONDS), stage) shouldBe DeadEnd
      LocalSoftDeadline(jDuration.ofNanos(nanos), stage) shouldBe DeadEnd
      LocalSoftDeadline(FiniteDuration(nanos, TimeUnit.NANOSECONDS))(stage) shouldBe DeadEnd
      LocalSoftDeadline(jDuration.ofNanos(nanos))(stage) shouldBe DeadEnd
    }

  it should "return the initial stage (tsSupplier == now) if the duration is positive" in
    forAll(Gen.choose(1L, Long.MaxValue)) { nanos =>
      val stage = mock[Stage[Any, Nothing, Nothing]]

      def test(lsdStage: Stage[Any, Nothing, Nothing]): Assertion =
        inside(lsdStage) { case LocalSoftDeadline(tsSupplier, now, `nanos`, `stage`) =>
          tsSupplier shouldBe now
          now() should be < now()
        }

      test(LocalSoftDeadline(FiniteDuration(nanos, TimeUnit.NANOSECONDS), stage))
      test(LocalSoftDeadline(jDuration.ofNanos(nanos), stage))
      test(LocalSoftDeadline(FiniteDuration(nanos, TimeUnit.NANOSECONDS))(stage))
      test(LocalSoftDeadline(jDuration.ofNanos(nanos))(stage))
    }

  it should "return a yield with Break status and the initial stage (tsSupplier == now) when overdue" in
    forAll(
      Gen.zip(
        Gen.long,
        Gen.choose(1L, Int.MaxValue),
        Gen.choose(1L, Int.MaxValue),
        Gen.uuid,
        arbEvolutionToYield[UUID, Instant, Long].arbitrary)) {
      case (ts, duration, overdue, in, yieldSupplier) =>
        val tsSupplier = mock[() => Long]("timestamp supplier")
        val now = mock[() => Long]("now")
        val stage = mock[Stage[UUID, Instant, Long]]("underlying stage")
        val evolution = mock[Evolution[UUID, Instant, Long]]("evolution")
        val yld = yieldSupplier(evolution)
        val lsd = LocalSoftDeadline(tsSupplier, now, duration, stage)

        def test(currentTS: Long): Unit = {
          inSequence {
            (tsSupplier.apply _).expects().returns(ts)
            (stage.apply _).expects(in).returns(yld)
            (now.apply _).expects().returns(currentTS)
          }
          val expectedStatus = yld.status.break
          val lsdYield = lsd(in)
          inside((yld, lsdYield)) {
            case (Yield.Some(expectedOut, _, _), Yield.Some(out, `expectedStatus`, _)) => out shouldEqual expectedOut
            case (Yield.None(_, _), Yield.None(`expectedStatus`, _)) => succeed
          }
          testWrappedEvolution(
            lsdYield.evolution, evolution, LocalSoftDeadline(now, now, duration, _: Stage[UUID, Instant, Long]))
        }

        test(ts + duration)
        test(ts + duration + overdue)
    }

  it should
    "return a yield with unchanged status and preserved timestamp on success, reset clock on complete/error when not overdue" in
    forAll(
      Gen.zip(
        Gen.long,
        Gen.choose(1L, Int.MaxValue),
        Gen.choose(1L, Int.MaxValue),
        Arbitrary.arbitrary[ZoneId],
        arbEvolutionToYield[ZoneId, ZonedDateTime, Exception].arbitrary
      )) {
      case (ts, spent, rest, in, yieldSupplier) =>
        val duration = spent + rest
        val tsSupplier = mock[() => Long]("timestamp supplier")
        val now = mock[() => Long]("now")
        val stage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("underlying stage")
        val evolution = mock[Evolution[ZoneId, ZonedDateTime, Exception]]("evolution")
        val yld = yieldSupplier(evolution)
        val lsd = LocalSoftDeadline(tsSupplier, now, duration, stage)

        def test(currentTS: Long): Assertion = {
          inSequence {
            (tsSupplier.apply _).expects().returns(ts)
            (stage.apply _).expects(in).returns(yld)
            (now.apply _).expects().returns(currentTS)
          }
          val lsdYield = lsd(in)
          inside((yld, lsdYield)) {
            case (Yield.Some(expectedOut, _, _), Yield.Some(out, yld.`status`, _)) => out shouldEqual expectedOut
            case (Yield.None(_, _), Yield.None(yld.`status`, _)) => succeed
          }

          val onSuccessStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onSuccess stage")
          (evolution.onSuccess _).expects().returns(onSuccessStage)
          inside(lsdYield.evolution.onSuccess()) {
            case LocalSoftDeadline(tsSupplier, `now`, `duration`, `onSuccessStage`) =>
              tsSupplier() shouldBe ts
          }

          val onCompleteStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onComplete stage")
          (evolution.onComplete _).expects().returns(onCompleteStage)
          lsdYield.evolution.onComplete() shouldBe LocalSoftDeadline(now, now, duration, onCompleteStage)

          val onErrorStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onError stage")
          (evolution.onError _).expects().returns(onErrorStage)
          lsdYield.evolution.onError() shouldBe LocalSoftDeadline(now, now, duration, onErrorStage)
        }

        test(ts)
        test(ts + spent)
    }

  it should "call the alterand.skip() method" in {
    val tsSupplier = mock[() => Long]("timestamp supplier")
    val now = mock[() => Long]("now")
    val duration = 42
    val stage = mock[Stage[Long, UUID, Exception]]("alterand")
    val evolution = mock[Evolution[Long, UUID, Exception]]("evolution")
    inSequence {
      (stage.skip _).expects().returns(evolution)
      val lsdEvolution = LocalSoftDeadline[Long, UUID, Exception](tsSupplier, now, duration, stage).skip()

      val onSuccessStage = mock[Stage[Long, UUID, Exception]]
      (evolution.onSuccess _).expects().returns(onSuccessStage)
      lsdEvolution.onSuccess() shouldBe LocalSoftDeadline(tsSupplier, now, duration, onSuccessStage)

      val onCompleteStage = mock[Stage[Long, UUID, Exception]]
      (evolution.onComplete _).expects().returns(onCompleteStage)
      lsdEvolution.onComplete() shouldBe LocalSoftDeadline(now, now, duration, onCompleteStage)

      val onErrorStage = mock[Stage[Long, UUID, Exception]]
      (evolution.onError _).expects().returns(onErrorStage)
      lsdEvolution.onError() shouldBe LocalSoftDeadline(now, now, duration, onErrorStage)
    }
  }

  "Evolution" should "return Tail on success and Head on complete and on error" in
    forAll(Gen.zip(Gen.function0(Gen.long), Gen.posNum[Long])) { case (tsSupplier, duration) =>
      val now = mock[() => Long]("now")
      val evolution = mock[Evolution[Any, Nothing, Nothing]]("evolution")
      val _evolution = LocalSoftDeadline.Evolution(tsSupplier, now, duration, evolution)
      testWrappedEvolution(
        _evolution,
        evolution,
        LocalSoftDeadline(tsSupplier, now, duration, _: Stage[Any, Nothing, Nothing]),
        LocalSoftDeadline(now, now, duration, _: Stage[Any, Nothing, Nothing]),
        LocalSoftDeadline(now, now, duration, _: Stage[Any, Nothing, Nothing])
      )
    }
}
