package h8io.stages.base

import h8io.stages.{Evolution, Stage, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertions
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlteratorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call alterand's dispose" in {
    val inner = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = new TestAlterator(inner)
    (inner.dispose _).expects()
    noException should be thrownBy alterator.dispose()
  }

  it should "propagate alterand's dispose exception" in {
    val inner = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = new TestAlterator(inner)
    val error = new RuntimeException("dispose failed")
    (inner.dispose _).expects().throws(error)
    the[RuntimeException] thrownBy alterator.dispose() should be(error)
  }
}

private class TestAlterator(val alterand: Stage[Any, Nothing, Nothing])
    extends Alterator[Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
  override def apply(in: Any): Yield[Any, Nothing, Nothing] = Assertions.fail("apply should not be called")
  override def skip(): Evolution[Any, Nothing, Nothing] = Assertions.fail("skip should not be called")
}
