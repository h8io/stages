package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class IdentityTest extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "Identity" should "return input" in
    forAll((in: String) => Identity(in) shouldBe Yield.Some(`in`, Signal.Success, Identity))
}
