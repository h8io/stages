package h8io.stages.operators

import h8io.stages.*
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
    with StagesCoreTestUtil {
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
        val stage = mock[Stage[UUID, Instant, Long]]("alterand")
        val evolution = mock[Evolution[UUID, Instant, Long]]("evolution")
        val yld = yieldSupplier(evolution)
        val lsd = LocalSoftDeadline(tsSupplier, now, duration, stage)

        def test(currentTS: Long): Unit = {
          inSequence {
            (tsSupplier.apply _).expects().returns(ts)
            (stage.apply _).expects(in).returns(yld)
            (now.apply _).expects().returns(currentTS)
          }
          val expectedStatus = if (yld.status == Status.Success) Status.complete else yld.status
          val lsdYield = lsd(in)
          inside((yld, lsdYield)) {
            case (Yield.Some(expectedOut, _, _), Yield.Some(out, `expectedStatus`, _)) => out shouldEqual expectedOut
            case (Yield.None(_, _), Yield.None(`expectedStatus`, _)) => succeed
          }
          testMappedEvolution(
            lsdYield.evolution, evolution, LocalSoftDeadline(now, now, duration, _: Stage[UUID, Instant, Long]))
        }

        test(ts + duration)
        test(ts + duration + overdue)
    }

  it should
    "return a yield with unchanged status and preserved timestamp on success, reset clock on Complete when not overdue" in
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
        val stage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("alterand")
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
          (evolution.evolve _).expects(Status.Success).returns(onSuccessStage)
          inside(lsdYield.evolution.evolve(Status.Success)) {
            case LocalSoftDeadline(tsSupplier, `now`, `duration`, `onSuccessStage`) => tsSupplier() shouldBe ts
          }

          val onCompleteStage = mock[Stage[ZoneId, ZonedDateTime, Exception]]("onComplete stage")
          val complete = mockComplete()
          (evolution.evolve _).expects(complete).returns(onCompleteStage)
          lsdYield.evolution.evolve(complete) shouldBe LocalSoftDeadline(now, now, duration, onCompleteStage)

          (evolution.dispose _).expects()
          noException should be thrownBy lsdYield.evolution.dispose()
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

      val onSuccessStage = mock[Stage[Long, UUID, Exception]]("onSuccess stage")
      (evolution.evolve _).expects(Status.Success).returns(onSuccessStage)
      lsdEvolution.evolve(Status.Success) shouldBe LocalSoftDeadline(tsSupplier, now, duration, onSuccessStage)

      val onCompleteStage = mock[Stage[Long, UUID, Exception]]("onComplete stage")
      val complete = mockComplete()
      (evolution.evolve _).expects(complete).returns(onCompleteStage)
      lsdEvolution.evolve(complete) shouldBe LocalSoftDeadline(now, now, duration, onCompleteStage)

      (evolution.dispose _).expects()
      noException should be thrownBy lsdEvolution.dispose()
    }
  }

  "Evolution" should "call the corresponding method on the inner evolution" in
    forAll(Gen.posNum[Long]) { duration =>
      val now = mock[() => Long]("now")
      val tsSupplier = mock[() => Long]("timestamp supplier")
      val evolution = mock[Evolution[Any, Nothing, Nothing]]("evolution")
      val lsdEvolution = LocalSoftDeadline.Evolution(tsSupplier, now, duration, evolution)

      inSequence {
        val onSuccessStage = mock[Stage[Any, Nothing, Nothing]]("onSuccess stage")
        (evolution.evolve _).expects(Status.Success).returns(onSuccessStage)
        lsdEvolution.evolve(Status.Success) shouldBe LocalSoftDeadline(tsSupplier, now, duration, onSuccessStage)

        val onCompleteStage = mock[Stage[Any, Nothing, Nothing]]("onComplete stage")
        val complete = mockComplete()
        (evolution.evolve _).expects(complete).returns(onCompleteStage)
        lsdEvolution.evolve(complete) shouldBe LocalSoftDeadline(now, now, duration, onCompleteStage)

        (evolution.dispose _).expects()
        noException should be thrownBy lsdEvolution.dispose()
      }
    }
}
