package h8io.stages.base

import h8io.stages.Yield
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SAMStageTest extends AnyFlatSpec with Matchers {
  "SAMStage" should "return self from skip" in {
    val stage = new SAMStage[Int, String, Nothing] {
      override def apply(in: Int): Yield[Int, String, Nothing] = fail("apply should not be called")
    }
    stage.skip() shouldBe stage
  }
}
