package h8io.stages.base

import h8io.stages.Yield
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseStageTest extends AnyFlatSpec with Matchers {
  "BaseStage" should "return self from skip" in {
    val stage = new BaseStage[Int, String, Nothing] {
      override def apply(in: Int): Yield[Int, String, Nothing] = fail("apply should not be called")
    }
    stage.skip() shouldBe stage.toEvolution
  }
}
