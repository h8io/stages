package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConstEvolutionTest extends AnyFlatSpec with Matchers with MockFactory {
  "onSuccess" should "return inner stage" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    ConstEvolution(stage).onSuccess() should be theSameInstanceAs stage
  }

  "onComplete" should "return inner stage" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    ConstEvolution(stage).onComplete() should be theSameInstanceAs stage
  }

  "onError" should "return inner stage" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]
    ConstEvolution(stage).onError() should be theSameInstanceAs stage
  }
}
