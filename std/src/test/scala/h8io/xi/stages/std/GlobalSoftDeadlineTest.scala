package h8io.xi.stages.std

import h8io.xi.stages.{Signal, StagesCoreArbitraries, Yield}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Duration as jDuration
import java.util.UUID
import scala.annotation.tailrec
import scala.concurrent.duration.Duration

class GlobalSoftDeadlineTest
    extends AnyFlatSpec
    with Matchers
    with Inside
    with MockFactory
    with ScalaCheckPropertyChecks
    with StagesCoreArbitraries {
  "apply" should "create a GlobalSoftDeadline object from a duration" in
    forAll(Gen.choose(0, Long.MaxValue)) { nanos =>
      inside(GlobalSoftDeadline(Duration.fromNanos(nanos))) { case GlobalSoftDeadline(now, `nanos`) =>
        now() should be < now()
      }
      inside(GlobalSoftDeadline(jDuration.ofNanos(nanos))) { case GlobalSoftDeadline(now, `nanos`) =>
        now() should be < now()
      }
    }

  "GlobalSoftDeadline" should "return Complete signal on overdue" in
    forAll(Gen.zip(Gen.long, Gen.listOf(Gen.zip(Gen.choose(0L, 1000L), Gen.uuid)))) { case (ts, parameters) =>
      val maxDuration = parameters.iterator.map(_._1).sum
      forAll(Gen.choose(0, maxDuration)) { duration =>
        val now = mock[() => Long]
        (now.apply _).expects().returns(ts)
        val stage = GlobalSoftDeadline[UUID](now, duration)
        loop(ts, 0, parameters)

        @tailrec def loop(previousTS: Long, previousInterval: Long, parameters: List[(Long, UUID)]): Unit =
          parameters match {
            case head :: tail =>
              val (interval, in) = head
              val currentTS = previousTS + interval
              val currentInterval = previousInterval + interval
              (now.apply _).expects().returns(currentTS)
              inside(stage(in)) { case Yield.Some(`in`, signal, `stage`) =>
                if (currentInterval < duration) signal shouldBe Signal.Success else signal shouldBe Signal.Complete
              }
              loop(currentTS, currentInterval, tail)
            case Nil =>
          }
      }
    }
}
