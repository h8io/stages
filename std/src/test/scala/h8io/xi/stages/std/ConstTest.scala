package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ConstTest extends AnyFlatSpec with Matchers with MockFactory with ScalaCheckPropertyChecks {
  "Const" should "always return the same value" in
    forAll { (out: String) =>
      val stage = Const(out)
      stage(mock[AnyRef]) shouldBe Yield.Some(`out`, Signal.Success, `stage`)
    }
}
