package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BinaryOperatorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call beforeDispose, right's dispose, left's dispose, and afterDispose in order" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (stage.afterDispose _).expects(context)
    }
    noException should be thrownBy stage.dispose()
  }

  it should "call right and left dispose even if beforeDispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val exception = new Exception("beforeDispose failed")
    inSequence {
      (stage.beforeDispose _).expects().throws(exception)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    the[Exception] thrownBy stage.dispose() should be theSameInstanceAs exception
  }

  it should "suppress right dispose exception if beforeDispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val beforeDisposeException = new Exception("beforeDispose failed")
    val rightDisposeException = new Exception("right dispose failed")
    inSequence {
      (stage.beforeDispose _).expects().throws(beforeDisposeException)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs beforeDisposeException
    beforeDisposeException.getSuppressed should contain(rightDisposeException)
  }

  it should "suppress left dispose exception if beforeDispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val beforeDisposeException = new Exception("beforeDispose failed")
    val leftDisposeException = new Exception("left dispose failed")
    inSequence {
      (stage.beforeDispose _).expects().throws(beforeDisposeException)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs beforeDisposeException
    beforeDisposeException.getSuppressed should contain(leftDisposeException)
  }

  it should "suppress right and left dispose exceptions if beforeDispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val beforeDisposeException = new Exception("beforeDispose failed")
    val rightDisposeException = new Exception("right dispose failed")
    val leftDisposeException = new Exception("left dispose failed")
    inSequence {
      (stage.beforeDispose _).expects().throws(beforeDisposeException)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs beforeDisposeException
    beforeDisposeException.getSuppressed should contain(rightDisposeException)
    beforeDisposeException.getSuppressed should contain(leftDisposeException)
    beforeDisposeException.getSuppressed.length shouldBe 2
  }

  it should "call left dispose and afterDispose even if right dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val rightDisposeException = new Exception("right dispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (stage.afterDispose _).expects(context)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs rightDisposeException
    rightDisposeException.getSuppressed shouldBe empty
  }

  it should "call afterDispose even if left dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val leftDisposeException = new Exception("left dispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
      (stage.afterDispose _).expects(context)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs leftDisposeException
    leftDisposeException.getSuppressed shouldBe empty
  }

  it should "suppress left dispose exception if right dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val rightDisposeException = new Exception("right dispose failed")
    val leftDisposeException = new Exception("left dispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
      (stage.afterDispose _).expects(context)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs rightDisposeException
    rightDisposeException.getSuppressed should contain(leftDisposeException)
    rightDisposeException.getSuppressed.length shouldBe 1
  }

  it should "suppress afterDispose exception if right dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val rightDisposeException = new Exception("right dispose failed")
    val afterDisposeException = new Exception("afterDispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (stage.afterDispose _).expects(context).throws(afterDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs rightDisposeException
    rightDisposeException.getSuppressed should contain(afterDisposeException)
    rightDisposeException.getSuppressed.length shouldBe 1
  }

  it should "suppress afterDispose exception if left dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val leftDisposeException = new Exception("left dispose failed")
    val afterDisposeException = new Exception("afterDispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
      (stage.afterDispose _).expects(context).throws(afterDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs leftDisposeException
    leftDisposeException.getSuppressed should contain(afterDisposeException)
    leftDisposeException.getSuppressed.length shouldBe 1
  }

  it should "suppress left and afterDispose exceptions if right dispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val rightDisposeException = new Exception("right dispose failed")
    val leftDisposeException = new Exception("left dispose failed")
    val afterDisposeException = new Exception("afterDispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().throws(rightDisposeException)
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().throws(leftDisposeException)
      (stage.afterDispose _).expects(context).throws(afterDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs rightDisposeException
    rightDisposeException.getSuppressed should contain(leftDisposeException)
    rightDisposeException.getSuppressed should contain(afterDisposeException)
    rightDisposeException.getSuppressed.length shouldBe 2
  }

  it should "rethrow an exception if afterDispose throws" in {
    val left = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val right = mock[Stage[Any, Nothing, Nothing]]("right stage")
    val stage = mock[BinaryOperatorTest.TestStage]("binary stage")
    val context = new Object
    val afterDisposeException = new Exception("afterDispose failed")
    inSequence {
      (stage.beforeDispose _).expects().returns(context)
      (() => stage.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => stage.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (stage.afterDispose _).expects(context).throws(afterDisposeException)
    }
    val exception = the[Exception] thrownBy stage.dispose()
    exception should be theSameInstanceAs afterDisposeException
  }
}

object BinaryOperatorTest {
  trait TestStage
      extends BinaryOperator[Stage[Any, Nothing, Nothing], Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
    override type DisposeContext = AnyRef
  }
}
