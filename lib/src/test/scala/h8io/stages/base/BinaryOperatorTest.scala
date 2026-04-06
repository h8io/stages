package h8io.stages.base

import h8io.stages.Stage
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BinaryOperatorTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call right and left dispose in order" in {
    val (op, left, right) = mkMocks()
    inSequence {
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    noException should be thrownBy op.dispose()
  }

  it should "propagate right dispose exception and still call left dispose" in {
    val (op, left, right) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    inSequence {
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().returns(())
    }
    the[RuntimeException] thrownBy op.dispose() should be(rightError)
  }

  it should "suppress left dispose exception when right dispose already threw" in {
    val (op, left, right) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().throws(rightError)
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
    }
    val thrown = the[RuntimeException] thrownBy op.dispose()
    thrown should be(rightError)
    thrown.getSuppressed should contain(leftError)
  }

  it should "propagate left dispose exception" in {
    val (op, left, right) = mkMocks()
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (() => op.right).expects().returns(right)
      (right.dispose _).expects().returns(())
      (() => op.left).expects().returns(left)
      (left.dispose _).expects().throws(leftError)
    }
    the[RuntimeException] thrownBy op.dispose() should be(leftError)
  }

  private def mkMocks() = (
    mock[TestBinaryOperator]("op"),
    mock[Stage[Any, Nothing, Nothing]]("left"),
    mock[Stage[Any, Nothing, Nothing]]("right")
  )
}

private trait TestBinaryOperator
    extends BinaryOperator[Stage[Any, Nothing, Nothing], Stage[Any, Nothing, Nothing], Any, Nothing, Nothing]
