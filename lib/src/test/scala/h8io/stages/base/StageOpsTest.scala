package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageOpsTest extends AnyFlatSpec with Matchers with MockFactory {
  "toEvolution" should "return ConstEvolution" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]("stage")
    stage.toEvolution shouldBe ConstEvolution(stage)
    val dispose = mock[() => Unit]("dispose")
    stage.toEvolution(dispose) shouldBe ConstEvolution(stage, dispose)
  }
}
