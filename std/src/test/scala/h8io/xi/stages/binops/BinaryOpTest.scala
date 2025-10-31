package h8io.xi.stages.binops

import h8io.xi.stages.{Stage, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BinaryOpTest extends AnyFlatSpec with Matchers with MockFactory {
  "BinaryOp's dispose" should "call right stage's dispose and then left stage's dispose" in {
    val leftStage = mock[Stage[Any, Nothing, Nothing]]("left stage")
    val rightStage = mock[Stage[Any, Nothing, Nothing]]("right stage")
    inSequence {
      (rightStage.dispose _).expects()
      (leftStage.dispose _).expects()
    }
    noException should be thrownBy new BinaryOp[Any, Nothing, Nothing, Nothing, Nothing] {
      val left: Stage[Any, Nothing, Nothing] = leftStage
      val right: Stage[Any, Nothing, Nothing] = rightStage

      def apply(in: Any): Yield[Any, Nothing, Nothing] = fail("Method apply should not be called")
    }.dispose()
  }
}
