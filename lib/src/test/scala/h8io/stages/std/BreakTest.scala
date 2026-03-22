package h8io.stages.std

import h8io.stages.{Status, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BreakTest extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "Break" should "return Yield.Some with the input, Complete status, and idempotent evolution" in
    forAll((in: Long) => Break[Long](in) shouldBe Yield.Some(in, Status.Complete, Break))
}
