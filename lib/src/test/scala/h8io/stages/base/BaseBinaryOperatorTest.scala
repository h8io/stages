package h8io.stages.base

import h8io.stages.{Evolution, Stage, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseBinaryOperatorTest extends AnyFlatSpec with Matchers {
  "preDispose" should "return unit" in {
    baseBinaryOperator.preDispose() shouldBe (())
  }

  "postDispose" should "do nothing" in {
    noException should be thrownBy baseBinaryOperator.postDispose(())
  }

  private def baseBinaryOperator =
    new BaseBinaryOperator[Stage[Any, ?, ?], Stage[Any, ?, ?], Any, Nothing, Nothing] {
      override def left: Stage[Any, ?, ?] = fail("left should not be called")
      override def right: Stage[Any, ?, ?] = fail("right should not be called")
      override def apply(in: Any): Yield[Any, Nothing, Nothing] = fail("apply should not be called")
      override def skip(): Evolution[Any, Nothing, Nothing] = fail("skip should not be called")
    }
}
