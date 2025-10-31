package h8io.xi.stages.alterations

import h8io.xi.stages.*
import h8io.xi.stages.std.DeadEnd
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
    with StagesCoreTestUtil {
  "LocalSoftDeadline" should "return DeadEnd if Scala duration is not positive" in
    forAll(Gen.choose(Long.MinValue, 0L)) { nanos =>
      val stage = mock[Stage[Any, Nothing, Nothing]]
      LocalSoftDeadline(FiniteDuration(nanos, TimeUnit.NANOSECONDS), stage) shouldBe DeadEnd
      LocalSoftDeadline(jDuration.ofNanos(nanos), stage) shouldBe DeadEnd
      LocalSoftDeadline(FiniteDuration(nanos, TimeUnit.NANOSECONDS))(stage) shouldBe DeadEnd
      LocalSoftDeadline(jDuration.ofNanos(nanos))(stage) shouldBe DeadEnd
    }

  it should "return initial stage (tsSupplier == now) if duration is positive" in
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

  it should "return an yield with signal Break and initial stage (tsSupplier == now) if overdue" in
    forAll(
      Gen.zip(
        Gen.long,
        Gen.choose(1L, Int.MaxValue),
        Gen.choose(1L, Int.MaxValue),
        Gen.uuid,
        arbOnDoneToYield[UUID, Instant, Long].arbitrary)) {
      case (ts, duration, overdue, in, yieldSupplier) =>
        val tsSupplier = mock[() => Long]("timestamp supplier")
        val now = mock[() => Long]("now")
        val stage = mock[Stage[UUID, Instant, Long]]("underlying stage")
        val onDone = mock[OnDone[UUID, Instant, Long]]("onDone")
        val yld = yieldSupplier(onDone)
        val lsd = LocalSoftDeadline(tsSupplier, now, duration, stage)

        def test(currentTS: Long): Unit = {
          inSequence {
            (tsSupplier.apply _).expects().returns(ts)
            (stage.apply _).expects(in).returns(yld)
            (now.apply _).expects().returns(currentTS)
          }
          val expectedSignal = yld.signal.break
          val lsdYield = lsd(in)
          inside((yld, lsdYield)) {
            case (Yield.Some(expectedOut, _, _), Yield.Some(out, `expectedSignal`, _)) => out shouldEqual expectedOut
            case (Yield.None(_, _), Yield.None(`expectedSignal`, _)) => succeed
          }
          testWrappedOnDone(
            lsdYield.onDone, onDone, LocalSoftDeadline(now, now, duration, _: Stage[UUID, Instant, Long]))
        }

        test(ts + duration)
        test(ts + duration + overdue)
    }

  it should "return an yield with signal Break and initial stage (tsSupplier == now) if no overdue" in
    forAll(
      Gen.zip(
        Gen.long,
        Gen.choose(1L, Int.MaxValue),
        Gen.choose(1L, Int.MaxValue),
        Arbitrary.arbitrary[ZoneId],
        arbOnDoneToYield[ZoneId, ZonedDateTime, Exception].arbitrary
      )) {
      case (ts, spent, rest, in, yieldSupplier) =>
        val duration = spent + rest
        val tsSupplier = mock[() => Long]("timestamp supplier")
        val now = mock[() => Long]("now")
        val stage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("underlying stage")
        val onDone = mock[OnDone[ZoneId, ZonedDateTime, Exception]]("onDone")
        val yld = yieldSupplier(onDone)
        val lsd = LocalSoftDeadline(tsSupplier, now, duration, stage)

        def test(currentTS: Long): Assertion = {
          inSequence {
            (tsSupplier.apply _).expects().returns(ts)
            (stage.apply _).expects(in).returns(yld)
            (now.apply _).expects().returns(currentTS)
          }
          val lsdYield = lsd(in)
          inside((yld, lsdYield)) {
            case (Yield.Some(expectedOut, _, _), Yield.Some(out, yld.signal, _)) => out shouldEqual expectedOut
            case (Yield.None(_, _), Yield.None(yld.signal, _)) => succeed
          }

          val onSuccessStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onSuccess stage")
          (onDone.onSuccess _).expects().returns(onSuccessStage)
          inside(lsdYield.onDone.onSuccess()) {
            case LocalSoftDeadline(tsSupplier, `now`, `duration`, `onSuccessStage`) =>
              tsSupplier() shouldBe ts
          }

          val onCompleteStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onComplete stage")
          (onDone.onComplete _).expects().returns(onCompleteStage)
          lsdYield.onDone.onComplete() shouldBe LocalSoftDeadline(now, now, duration, onCompleteStage)

          val onErrorStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onError stage")
          (onDone.onError _).expects().returns(onErrorStage)
          lsdYield.onDone.onError() shouldBe LocalSoftDeadline(now, now, duration, onErrorStage)
        }

        test(ts)
        test(ts + spent)
    }

  "_OnDone" should "return Tail on success and Head on complete and on error" in
    forAll(Gen.zip(Gen.function0(Gen.long), Gen.posNum[Long])) { case (tsSupplier, duration) =>
      val now = mock[() => Long]("now")
      val onDone = mock[OnDone[Any, Nothing, Nothing]]("onDone")
      val _onDone = LocalSoftDeadline._OnDone(tsSupplier, now, duration, onDone)
      testWrappedOnDone(
        _onDone,
        onDone,
        LocalSoftDeadline(tsSupplier, now, duration, _: Stage[Any, Nothing, Nothing]),
        LocalSoftDeadline(now, now, duration, _: Stage[Any, Nothing, Nothing]),
        LocalSoftDeadline(now, now, duration, _: Stage[Any, Nothing, Nothing])
      )
    }
}
