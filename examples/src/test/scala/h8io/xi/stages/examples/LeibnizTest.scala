package h8io.xi.stages.examples

import h8io.xi.stages.{Signal, Yield}
import org.scalacheck.Gen
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration.DurationInt

class LeibnizTest extends AnyFlatSpec with Matchers with Inside with ScalaCheckPropertyChecks {
  "Leibniz series" should "be calculated for stage1" in
    inside(Leibniz.stage1(300.milliseconds)(())) { case Yield.Some(pi, Signal.Success, _) =>
      pi shouldEqual (math.Pi +- 0.01)
    }

  it should "be calculated for stage2" in
    inside(Leibniz.stage2(300.milliseconds)(())) { case Yield.Some(pi, Signal.Success, _) =>
      pi shouldEqual (math.Pi +- 0.01)
    }

  "Main stage" should "return the initial stage on error and on complete" in
    forAll(Gen.zip(Gen.long, Gen.double, Gen.double)) { case (n, t, s) =>
      Leibniz.Pi(n, t, s).onError() shouldBe Leibniz.InitialStage
    }
}
