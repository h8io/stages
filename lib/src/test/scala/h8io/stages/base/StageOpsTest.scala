package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageOpsTest extends AnyFlatSpec with Matchers with MockFactory with Inside {
  "toEvolution" should "return ConstEvolution" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]("stage")
    inside(stage.toEvolution) { case ConstEvolution(innerStage) =>
      innerStage should be theSameInstanceAs stage
    }
  }
}
