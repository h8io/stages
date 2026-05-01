package h8io.stages.examples

import h8io.stages.{Outcome, Status}
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration.DurationInt

class LeibnizTest extends AnyFlatSpec with Matchers with Inside with ScalaCheckPropertyChecks {
  "Leibniz series" should "be calculated for stage1" in
    inside(Leibniz.pipeline1(300.milliseconds).execute(())) { case Outcome.Some(pi, Status.Success, None) =>
      pi shouldEqual (math.Pi +- 0.01)
    }

  it should "be calculated for stage2" in
    inside(Leibniz.pipeline2(300.milliseconds).execute(())) { case Outcome.Some(pi, Status.Success, None) =>
      pi shouldEqual (math.Pi +- 0.01)
    }
}
