package h8io.xi.stages.projections

import h8io.xi.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Tuple2Test extends AnyFlatSpec with Matchers with MockFactory {
  "Left" should "return the first element of the tuple" in {
    val left = mock[AnyRef]
    Tuple2.Left((left, mock[AnyRef])) shouldBe Yield.Some(left, Signal.Success, Tuple2.Left)
  }

  "Right" should "return the second element of the tuple" in {
    val right = mock[AnyRef]
    Tuple2.Right((mock[AnyRef], right)) shouldBe Yield.Some(right, Signal.Success, Tuple2.Right)
  }
}
