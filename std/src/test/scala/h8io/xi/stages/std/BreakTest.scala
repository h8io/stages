package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class BreakTest extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "Break" should "return Yield.Some with input, Complete signal and idempotent onDone" in
    forAll((in: Long) => Break[Long](in) shouldBe Yield.Some(in, Signal.Complete, Break))
}
