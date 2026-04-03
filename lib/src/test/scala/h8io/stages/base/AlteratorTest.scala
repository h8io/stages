package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlteratorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call alterand's dispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[TestAlterator]("alterator")
    val context = mock[AnyRef]("context")
    inSequence {
      (alterator.preDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
      (alterator.postDispose _).expects(context)
    }
    noException should be thrownBy alterator.dispose()
  }

  it should "propagate preDispose exception and still call alterand's dispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[TestAlterator]("alterator")
    val error = new RuntimeException("preDispose")
    inSequence {
      (alterator.preDispose _).expects().throws(error)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
    }
    the[RuntimeException] thrownBy alterator.dispose() should be(error)
  }

  it should "suppress alterand's dispose exception when preDispose already threw" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[TestAlterator]("alterator")
    val preError = new RuntimeException("preDispose")
    val disposeError = new RuntimeException("dispose")
    inSequence {
      (alterator.preDispose _).expects().throws(preError)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().throws(disposeError)
    }
    val thrown = the[RuntimeException] thrownBy alterator.dispose()
    thrown should be(preError)
    thrown.getSuppressed should contain(disposeError)
  }

  it should "propagate alterand's dispose exception and still call postDispose" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[TestAlterator]("alterator")
    val context = mock[AnyRef]("context")
    val disposeError = new RuntimeException("dispose")
    inSequence {
      (alterator.preDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().throws(disposeError)
      (alterator.postDispose _).expects(context).returns(())
    }
    the[RuntimeException] thrownBy alterator.dispose() should be(disposeError)
  }

  it should "suppress postDispose exception when alterand's dispose already threw" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[TestAlterator]("alterator")
    val context = mock[AnyRef]("context")
    val disposeError = new RuntimeException("dispose")
    val postError = new RuntimeException("postDispose")
    inSequence {
      (alterator.preDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().throws(disposeError)
      (alterator.postDispose _).expects(context).throws(postError)
    }
    val thrown = the[RuntimeException] thrownBy alterator.dispose()
    thrown should be(disposeError)
    thrown.getSuppressed should contain(postError)
  }

  it should "propagate postDispose exception when preDispose and alterand's dispose succeeded" in {
    val alterand = mock[Stage[Any, Nothing, Nothing]]("alterand")
    val alterator = mock[TestAlterator]("alterator")
    val context = mock[AnyRef]("context")
    val postError = new RuntimeException("postDispose")
    inSequence {
      (alterator.preDispose _).expects().returns(context)
      (() => alterator.alterand).expects().returns(alterand)
      (alterand.dispose _).expects().returns(())
      (alterator.postDispose _).expects(context).throws(postError)
    }
    the[RuntimeException] thrownBy alterator.dispose() should be(postError)
  }
}

private trait TestAlterator extends Alterator[Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
  override type DisposeContext = AnyRef
}
