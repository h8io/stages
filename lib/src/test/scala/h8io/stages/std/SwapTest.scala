package h8io.stages.std

import h8io.stages.{Status, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SwapTest extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "Swap" should "swap the two elements of the pair" in
    forAll((a: String, b: Int) => Swap[String, Int]((a, b)) shouldBe Yield.Some((b, a), Status.Success, Swap))

  it should "be a polymorphic singleton" in {
    Swap[String, Int] should be theSameInstanceAs Swap
  }
}
