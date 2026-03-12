package h8io.stages.base

import h8io.stages.Yield
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageWithOnDoneTest extends AnyFlatSpec with Matchers {
  "OnDoneForStage" should "return self for any signal" in {
    val stage = new StageWithOnDone[Int, String, Nothing] {
      def apply(in: Int): Yield[Int, String, Nothing] = fail("apply should not be called")
    }

    stage.onSuccess() should be theSameInstanceAs stage
    stage.onComplete() should be theSameInstanceAs stage
    stage.onError() should be theSameInstanceAs stage
  }
}
