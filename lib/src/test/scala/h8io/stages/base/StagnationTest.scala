package h8io.stages.base

import h8io.stages.{Stage, StagesCoreTestUtil, Status}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StagnationTest extends AnyFlatSpec with Matchers with MockFactory with StagesCoreTestUtil {
  "Stagnation" should "return self in apply" in {
    val stage = mock[StageWithStagnation[Any, Nothing, Nothing]]
    stage.evolve(Status.Success) shouldBe stage
    stage.evolve(mockComplete()) shouldBe stage
    noException should be thrownBy stage.dispose()
  }
}

private trait StageWithStagnation[-I, +O, +E] extends Stage[I, O, E] with Stagnation[I, O, E] {
  override final def dispose(): Unit = super.dispose()
}
