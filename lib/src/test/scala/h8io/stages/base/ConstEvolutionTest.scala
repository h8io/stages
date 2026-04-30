package h8io.stages.base

import h8io.stages.{Stage, StagesCoreTestUtil}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConstEvolutionTest extends AnyFlatSpec with Matchers with MockFactory with StagesCoreTestUtil {
  "apply" should "always return inner stage" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    testConstEvolution(ConstEvolution(stage), stage)
  }

  "dispose" should "not throw exceptions" in {
    noException should be thrownBy mock[Stage[Any, Nothing, Nothing]].toEvolution.dispose()
  }

  it should "call the nested dispose" in {
    val dispose = mock[() => Unit]
    (dispose.apply _).expects()
    noException should be thrownBy ConstEvolution(mock[Stage[Any, Nothing, Nothing]], dispose).dispose()
  }
}
