package h8io.stages.examples

import h8io.stages.{Outcome, Status}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration.DurationInt

class WallisTest extends AnyFlatSpec with Matchers with Inside with MockFactory with ScalaCheckPropertyChecks {
  "Wallis product" should "be calculated" in {
    val stage = Wallis.pipeline(300.milliseconds)
    inside(stage.execute(mock[AnyRef])) { case Outcome.Some(pi, Status.Success, None) =>
      pi shouldEqual (math.Pi +- 0.01)
    }
  }
}
