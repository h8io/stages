package h8io.stages.base

import h8io.stages.{Stage, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseEvolutionTest extends AnyFlatSpec with Matchers {
  "EvolutionForStage" should "return self for any status" in {
    val stage = new Stage[Int, String, Nothing] with BaseEvolution[Int, String, Nothing] {
      def apply(in: Int): Yield[Int, String, Nothing] = fail("apply should not be called")
    }

    stage.onSuccess() should be theSameInstanceAs stage
    stage.onComplete() should be theSameInstanceAs stage
    stage.onError() should be theSameInstanceAs stage
  }
}
