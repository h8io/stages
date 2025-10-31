package h8io.xi.stages.std

import h8io.xi.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UnliftTest extends AnyFlatSpec with Matchers with MockFactory {
  "Unlift" should "return Yield.Some for Some input" in {
    val value = mock[AnyRef]
    Unlift[AnyRef](Some(value)) should matchPattern { case Yield.Some(`value`, Signal.Success, Unlift) => }
  }

  it should "return Yield.None for None input" in {
    Unlift[AnyRef](None) should matchPattern { case Yield.None(Signal.Success, Unlift) => }
  }
}
