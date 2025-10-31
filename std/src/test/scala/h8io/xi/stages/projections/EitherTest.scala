package h8io.xi.stages.projections

import h8io.xi.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EitherTest extends AnyFlatSpec with Matchers with MockFactory {
  "Left" should "return Yield.Some if the input is scala.util.Left" in {
    val value = mock[AnyRef]
    Either.Left(Left(value)) shouldBe Yield.Some(value, Signal.Success, Either.Left)
  }

  it should "return Yield.None if the input is scala.util.Right" in {
    Either.Left[AnyRef].apply(Right(mock[AnyRef])) shouldBe Yield.None(Signal.Success, Either.Left)
  }

  "Right" should "return Yield.None if the input is scala.util.Right" in {
    Either.Right[AnyRef].apply(Left(mock[AnyRef])) shouldBe Yield.None(Signal.Success, Either.Right)
  }

  it should "return Yield.Some if the input is scala.util.Right" in {
    val value = mock[AnyRef]
    Either.Right(Right(value)) shouldBe Yield.Some(value, Signal.Success, Either.Right)
  }
}
