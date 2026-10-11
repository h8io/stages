package h8io.stages.projections

import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Tuple2Test extends AnyFlatSpec with Matchers with MockFactory {
  "Left" should "return the first element of the tuple" in {
    val left = new AnyRef
    Tuple2.Left[AnyRef]((left, new AnyRef)) shouldBe Yield.Some(left, Status.Success, Tuple2.Left)
  }

  "Right" should "return the second element of the tuple" in {
    val right = new AnyRef
    Tuple2.Right[AnyRef]((new AnyRef, right)) shouldBe Yield.Some(right, Status.Success, Tuple2.Right)
  }
}
