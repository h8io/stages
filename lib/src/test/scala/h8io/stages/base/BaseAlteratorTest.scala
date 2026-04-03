package h8io.stages.base

import h8io.stages.{Stage, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseAlteratorTest extends AnyFlatSpec with Matchers {
  "preDispose" should "return unit" in {
    baseAlterator.preDispose() shouldBe (())
  }

  "postDispose" should "do nothing" in {
    noException should be thrownBy baseAlterator.postDispose(())
  }

  private def baseAlterator =
    new BaseAlterator[Stage.Any, Any, Nothing, Nothing] {
      override def alterand: Stage.Any = fail("alterand should not be called")
      override def apply(in: Any): Yield[Any, Nothing, Nothing] = fail("apply should not be called")
    }
}
