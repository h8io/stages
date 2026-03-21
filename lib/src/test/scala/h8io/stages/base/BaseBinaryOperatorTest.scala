package h8io.stages.base

import h8io.stages.{Stage, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseBinaryOperatorTest extends AnyFlatSpec with Matchers {
  private final class TestStage
      extends BaseBinaryOperator[
        Stage[Int, Int, Nothing],
        Stage[Int, Long, Nothing],
        Int,
        (String, String),
        Nothing] {
    override val left: Stage[Int, Int, Nothing] = (_: Int) => fail("apply should not be called")
    override val right: Stage[Int, Long, Nothing] = (_: Int) => fail("apply should not be called")
    override def apply(in: Int): Yield[Int, (String, String), Nothing] = fail("apply should not be called")
  }

  "BaseBinaryStage" should "use Unit dispose context by default" in {
    val stage = new TestStage
    val ctx: stage.DisposeContext = stage.beforeDispose()
    ctx shouldBe (())
    noException should be thrownBy stage.afterDispose(ctx)
  }

  it should "dispose without overriding beforeDispose/afterDispose" in {
    noException should be thrownBy new TestStage().dispose()
  }
}
