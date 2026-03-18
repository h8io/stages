package h8io.stages.base

import h8io.stages.{Stage, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseAlteratorTest extends AnyFlatSpec with Matchers {
  private final class TestAlterator extends BaseAlterator[Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
    override val alterand: Stage[Any, Nothing, Nothing] = (_: Any) => fail("apply should not be called")

    override def apply(in: Any): Yield[Any, Nothing, Nothing] = fail("apply should not be called")
  }

  "BaseAlterator" should "use Unit dispose context by default" in {
    val alterator = new TestAlterator
    val ctx: alterator.DisposeContext = alterator.beforeDispose()
    ctx shouldBe (())
    noException should be thrownBy alterator.afterDispose(ctx)
  }

  it should "dispose without overriding beforeDispose/afterDispose" in {
    noException should be thrownBy new TestAlterator().dispose()
  }
}
