package h8io.stages.projections

import h8io.stages.base.StageOps
import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Tuple2Test extends AnyFlatSpec with Matchers with MockFactory {
  "Left" should "return the first element of the tuple" in {
    val left = mock[AnyRef]
    Tuple2.Left((left, mock[AnyRef])) shouldBe Yield.Some(left, Status.Success, Tuple2.Left.toEvolution)
  }

  "Right" should "return the second element of the tuple" in {
    val right = mock[AnyRef]
    Tuple2.Right((mock[AnyRef], right)) shouldBe Yield.Some(right, Status.Success, Tuple2.Right.toEvolution)
  }
}
