package h8io.xi.stages.examples

import h8io.xi.stages.{Signal, Yield}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration.DurationInt

class WallisTest extends AnyFlatSpec with Matchers with Inside with MockFactory with ScalaCheckPropertyChecks {
  "Wallis product" should "be calculated" in {
    val stage = Wallis.stage(300.milliseconds)
    inside(stage(mock[AnyRef])) { case Yield.Some(pi, Signal.Success, _) => pi shouldEqual (math.Pi +- 0.01) }
  }

  "Main stage" should "return the initial stage on error" in
    forAll(Gen.long)(n => Wallis.Pi(n).onError() shouldBe Wallis.InitialStage)
}
