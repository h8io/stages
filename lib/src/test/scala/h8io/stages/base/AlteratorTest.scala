package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlteratorTest extends AnyFlatSpec with Matchers with MockFactory {
  "Alterator's dispose" should "call beforeDispose, alterand's dispose, and afterDispose in order" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val context = new Object
    inSequence {
      (alterator.beforeDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
      (alterator.afterDispose _).expects(context)
    }
    noException should be thrownBy alterator.dispose()
  }

  it should "call alterand's dispose even if beforeDispose throws" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val exception = new Exception("beforeDispose failed")
    inSequence {
      (alterator.beforeDispose _).expects().throws(exception)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
    }
    the[Exception] thrownBy alterator.dispose() should be theSameInstanceAs exception
  }

  it should "call beforeDispose and afterDispose even if alterand's dispose throws" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val context = new Object
    val exception = new Exception("alterand's dispose failed")
    inSequence {
      (alterator.beforeDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().throws(exception)
      (alterator.afterDispose _).expects(context)
    }
    the[Exception] thrownBy alterator.dispose() should be theSameInstanceAs exception
  }

  it should "rethrow an exception if afterDispose throws" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val context = new Object
    val exception = new Exception("afterDispose failed")
    inSequence {
      (alterator.beforeDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
      (alterator.afterDispose _).expects(context).throws(exception)
    }
    the[Exception] thrownBy alterator.dispose() should be theSameInstanceAs exception
  }

  it should "suppress alterand's dispose exception if beforeDispose throws" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val beforeDisposeException = new Exception("beforeDispose failed")
    val alterandDisposeException = new Exception("alterand's dispose failed")
    inSequence {
      (alterator.beforeDispose _).expects().throws(beforeDisposeException)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().throws(alterandDisposeException)
    }
    val exception = the[Exception] thrownBy alterator.dispose()
    exception should be theSameInstanceAs beforeDisposeException
    beforeDisposeException.getSuppressed should contain(alterandDisposeException)
  }

  it should "suppress afterDispose exception if alterand's dispose throws" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[AlteratorTest.TestAlterator]("alterator")
    val context = new Object
    val alterandDisposeException = new Exception("alterand's dispose failed")
    val afterDisposeException = new Exception("afterDispose failed")
    inSequence {
      (alterator.beforeDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().throws(alterandDisposeException)
      (alterator.afterDispose _).expects(context).throws(afterDisposeException)
    }
    val exception = the[Exception] thrownBy alterator.dispose()
    exception should be theSameInstanceAs alterandDisposeException
    alterandDisposeException.getSuppressed should contain(afterDisposeException)
  }
}

object AlteratorTest {
  trait TestAlterator extends Alterator[Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
    override final type DisposeContext = AnyRef
  }
}
