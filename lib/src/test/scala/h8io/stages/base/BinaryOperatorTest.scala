package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BinaryOperatorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call right and left dispose in order, then postDispose" in {
    val (op, left, right, context) = mkMocks()
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (op.postDispose _).expects(context)
    }
    noException should be thrownBy op.dispose()
  }

  // preDispose throws

  it should "propagate preDispose exception, still call right and left dispose, skip postDispose" in {
    val (op, left, right, _) = mkMocks()
    val preError = new RuntimeException("preDispose")
    inSequence {
      (op.preDispose _).expects().throws(preError)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    the[RuntimeException] thrownBy op.dispose() should be(preError)
  }

  it should "suppress right dispose exception when preDispose already threw" in {
    val (op, left, right, _) = mkMocks()
    val preError = new RuntimeException("preDispose")
    val rightError = new RuntimeException("right dispose")
    inSequence {
      (op.preDispose _).expects().throws(preError)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(preError)
    thrown.getSuppressed should contain(rightError)
  }

  it should "suppress left dispose exception when preDispose already threw" in {
    val (op, left, right, _) = mkMocks()
    val preError = new RuntimeException("preDispose")
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (op.preDispose _).expects().throws(preError)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(preError)
    thrown.getSuppressed should contain(leftError)
  }

  it should "suppress both right and left dispose exceptions when preDispose already threw" in {
    val (op, left, right, _) = mkMocks()
    val preError = new RuntimeException("preDispose")
    val rightError = new RuntimeException("right dispose")
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (op.preDispose _).expects().throws(preError)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(preError)
    thrown.getSuppressed should contain(rightError)
    thrown.getSuppressed should contain(leftError)
  }

  // right.dispose throws

  it should "propagate right dispose exception, still call left dispose and postDispose" in {
    val (op, left, right, context) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (op.postDispose _).expects(context).returns(())
    }
    the[RuntimeException] thrownBy op.dispose() should be(rightError)
  }

  it should "suppress left dispose exception when right dispose already threw" in {
    val (op, left, right, context) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
      (op.postDispose _).expects(context).returns(())
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(rightError)
    thrown.getSuppressed should contain(leftError)
  }

  it should "suppress postDispose exception when right dispose already threw" in {
    val (op, left, right, context) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    val postError = new RuntimeException("postDispose")
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
      (op.postDispose _).expects(context).throws(postError)
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(rightError)
    thrown.getSuppressed should contain(postError)
  }

  it should "suppress both left dispose and postDispose exceptions when right dispose already threw" in {
    val (op, left, right, context) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    val leftError = new RuntimeException("left dispose")
    val postError = new RuntimeException("postDispose")
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
      (op.postDispose _).expects(context).throws(postError)
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(rightError)
    thrown.getSuppressed should contain(leftError)
    thrown.getSuppressed should contain(postError)
  }

  // left.dispose throws

  it should "propagate left dispose exception and still call postDispose" in {
    val (op, left, right, context) = mkMocks()
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
      (op.postDispose _).expects(context).returns(())
    }
    the[RuntimeException] thrownBy op.dispose() should be(leftError)
  }

  it should "suppress postDispose exception when left dispose already threw" in {
    val (op, left, right, context) = mkMocks()
    val leftError = new RuntimeException("left dispose")
    val postError = new RuntimeException("postDispose")
    inSequence {
      (op.preDispose _).expects().returns(context)
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
      (op.postDispose _).expects(context).throws(postError)
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(leftError)
    thrown.getSuppressed should contain(postError)
  }

  private def mkMocks() = (
    mock[TestBinaryOperator]("op"),
    mock[Stage[Any, Nothing, Nothing]]("left"),
    mock[Stage[Any, Nothing, Nothing]]("right"),
    mock[AnyRef]("context")
  )
}

private trait TestBinaryOperator
    extends BinaryOperator[Stage[Any, Nothing, Nothing], Stage[Any, Nothing, Nothing], Any, Nothing, Nothing] {
  override type DisposeContext = AnyRef
}
