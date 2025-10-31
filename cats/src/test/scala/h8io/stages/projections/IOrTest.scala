package h8io.stages.projections

import h8io.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IOrTest extends AnyFlatSpec with Matchers with MockFactory {
  "Left" should "return Yield.Some if the input is cats.data.Ior.Left" in {
    val value = mock[AnyRef]
    IOr.Left(cats.data.Ior.Left(value)) shouldBe Yield.Some(value, Signal.Success, IOr.Left)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Both" in {
    val value = mock[AnyRef]
    IOr.Left(cats.data.Ior.Both(value, mock[AnyRef])) shouldBe Yield.Some(value, Signal.Success, IOr.Left)
  }

  it should "return Yield.None if the input is cats.data.Ior.Right" in {
    IOr.Left[AnyRef].apply(cats.data.Ior.Right(mock[AnyRef])) shouldBe Yield.None(Signal.Success, IOr.Left)
  }

  "Right" should "return Yield.None if the input is cats.data.Ior.Left" in {
    IOr.Right[AnyRef].apply(cats.data.Ior.Left(mock[AnyRef])) shouldBe Yield.None(Signal.Success, IOr.Right)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Both" in {
    val value = mock[AnyRef]
    IOr.Right(cats.data.Ior.Both(mock[AnyRef], value)) shouldBe Yield.Some(value, Signal.Success, IOr.Right)
  }

  it should "return Yield.Some if the input is cats.data.Ior.Right" in {
    val value = mock[AnyRef]
    IOr.Right(cats.data.Ior.Right(value)) shouldBe Yield.Some(value, Signal.Success, IOr.Right)
  }
}
