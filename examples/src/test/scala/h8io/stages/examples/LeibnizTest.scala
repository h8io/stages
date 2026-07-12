package h8io.stages.examples

import h8io.stages.Status
import h8io.stages.base.*
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration.DurationInt

class LeibnizTest extends AnyFlatSpec with Matchers with Inside with ScalaCheckPropertyChecks {
  "Leibniz series" should "be calculated" in
    inside(Leibniz.pipeline(300.milliseconds).execute(())) { case Outcome.Some(pi, Status.Success, None) =>
      pi shouldEqual (math.Pi +- 0.01)
    }
}
