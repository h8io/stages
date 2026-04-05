package h8io.stages.base

import h8io.stages.Yield
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseStageTest extends AnyFlatSpec with Matchers {
  private val stage = new BaseStage[Int, String, Nothing] {
    override def apply(in: Int): Yield[Int, String, Nothing] = fail("apply should not be called")
  }

  "BaseStage" should "return self from skip" in {
    stage.skip() should be theSameInstanceAs stage
  }

  it should "not throw on dispose" in {
    noException should be thrownBy stage.dispose()
  }
}
