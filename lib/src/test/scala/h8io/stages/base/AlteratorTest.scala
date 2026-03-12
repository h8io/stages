package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlteratorTest extends AnyFlatSpec with Matchers with MockFactory {
  "Alterator's dispose" should "call underlying stage's dispose method" in {
    val stage = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val context = new Object
    inSequence {
      (alterator.beforeDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(stage)
      (stage.dispose _).expects().returns(())
      (alterator.afterDispose _).expects(context)
    }
    noException should be thrownBy alterator.dispose()
  }
}

object AlteratorTest {
  trait TestAlterator extends Alterator[Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
    override final type DisposeContext = AnyRef
  }
}
