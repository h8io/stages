package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlteratorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call alterand's dispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[Alterator[Stage[Any, Nothing, Nothing], Any, Nothing, Nothing]]("alterator")
    inSequence {
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
    }
    noException should be thrownBy alterator.dispose()
  }
}
