package h8io.stages.std

import h8io.stages.base.StageOps
import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UnliftTest extends AnyFlatSpec with Matchers with MockFactory {
  "Unlift" should "return Yield.Some for Some input" in {
    val value = mock[AnyRef]
    Unlift[AnyRef](Some(value)) shouldBe Yield.Some(`value`, Status.Success, Unlift.toEvolution)
  }

  it should "return Yield.None for None input" in {
    Unlift[AnyRef](None) shouldBe Yield.None(Status.Success, Unlift.toEvolution)
  }
}
