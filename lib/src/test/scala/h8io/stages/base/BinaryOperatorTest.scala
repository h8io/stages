package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BinaryOperatorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call right's dispose and left's dispose" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperator[Stage[Any, Nothing, Nothing], Stage[Any, Nothing, Nothing], Any, Nothing, Nothing]](
      "binary stage")
    inSequence {
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    noException should be thrownBy stage.dispose()
  }

  it should "call left's dispose even if right's dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperator[Stage[Any, Nothing, Nothing], Stage[Any, Nothing, Nothing], Any, Nothing, Nothing]](
      "binary stage")
    val exception = new Exception("right's dispose failed")
    inSequence {
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(exception)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    the[Exception] thrownBy stage.dispose() should be theSameInstanceAs exception
  }

  it should "suppress left's dispose exception if right's dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperator[Stage[Any, Nothing, Nothing], Stage[Any, Nothing, Nothing], Any, Nothing, Nothing]](
      "binary stage")
    val rightDisposeException = new Exception("right's dispose failed")
    val leftDisposeException = new Exception("lefty's dispose failed")
    inSequence {
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs rightDisposeException
    rightDisposeException.getSuppressed should contain(leftDisposeException)
  }
}
