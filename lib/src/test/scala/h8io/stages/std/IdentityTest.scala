package h8io.stages.std

import h8io.stages.base.StageOps
import h8io.stages.{Status, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class IdentityTest extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "Identity" should "return the input" in
    forAll((in: String) => Identity(in) shouldBe Yield.Some(`in`, Status.Success, Identity.toEvolution))

  it should "be a polymorphic singleton" in {
    Identity[String] should be theSameInstanceAs Identity
  }
}
