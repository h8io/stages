package h8io.stages.std

import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CompleteTest extends AnyFlatSpec with Matchers with MockFactory {
  "Complete" should "return Yield.Some with the input, Complete status, and idempotent evolution" in {
    val in = mock[AnyRef]
    Complete[AnyRef](in) shouldBe Yield.Some(in, Status.complete, Complete)
  }
}
