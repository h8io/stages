package h8io.stages.base

import h8io.stages.{Evolution, Stage}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertions.fail
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BaseBinaryOperatorEvolutionTest extends AnyFlatSpec with Matchers with MockFactory {
  "dispose" should "call right and left dispose in order" in {
    val (evolution, left, right) = mkMocks()
    inSequence {
      (right.dispose _).expects()
      (left.dispose _).expects()
      noException should be thrownBy evolution.dispose()
    }
  }

  it should "propagate right dispose exception and still call left dispose" in {
    val (evolution, left, right) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    inSequence {
      (right.dispose _).expects().throws(rightError)
      (left.dispose _).expects()
    }
    the[RuntimeException] thrownBy evolution.dispose() should be(rightError)
  }

  it should "suppress left dispose exception when right dispose already threw" in {
    val (evolution, left, right) = mkMocks()
    val rightError = new RuntimeException("right dispose")
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (right.dispose _).expects().throws(rightError)
      (left.dispose _).expects().throws(leftError)
    }
    val thrown = the[RuntimeException] thrownBy evolution.dispose()
    thrown should be(rightError)
    thrown.getSuppressed should contain(leftError)
  }

  it should "propagate left dispose exception" in {
    val (evolution, left, right) = mkMocks()
    val leftError = new RuntimeException("left dispose")
    inSequence {
      (right.dispose _).expects()
      (left.dispose _).expects().throws(leftError)
    }
    the[RuntimeException] thrownBy evolution.dispose() should be(leftError)
  }

  private def mkMocks() = {
    val left = mock[Evolution[Any, Nothing, Nothing]]("left")
    val right = mock[Evolution[Any, Nothing, Nothing]]("right")
    val evolution = TestBaseBinaryOperatorEvolution(left, right)
    (evolution, left, right)
  }
}

private final case class TestBaseBinaryOperatorEvolution(
    left: Evolution[Any, Nothing, Nothing],
    right: Evolution[Any, Nothing, Nothing])
    extends BaseBinaryOperator.Evolution[Any, Nothing, Nothing, Nothing, Nothing] {
  override protected def apply[_I <: Any, _E >: Nothing](
      leftStage: Stage[_I, Nothing, _E],
      rightStage: Stage[_I, Nothing, _E]): Stage[_I, Nothing, _E] = fail("apply should not be called")
}
