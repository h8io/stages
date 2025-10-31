package h8io.xi.stages.projections

import h8io.xi.stages.{Signal, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ValidatedTest extends AnyFlatSpec with Matchers with MockFactory {
  "Invalid" should "return Yield.Some if the input is Validated.Invalid" in {
    val value = mock[AnyRef]
    Validated.Invalid(cats.data.Validated.Invalid(value)) shouldBe Yield.Some(value, Signal.Success, Validated.Invalid)
  }

  it should "return Yield.None if the input is Validated.Valid" in {
    Validated.Invalid[AnyRef].apply(cats.data.Validated.Valid(mock[AnyRef])) shouldBe
      Yield.None(Signal.Success, Validated.Invalid)
  }

  "Valid" should "return Yield.None if the input is Validated.Invalid" in {
    Validated.Valid[AnyRef].apply(cats.data.Validated.Invalid(mock[AnyRef])) shouldBe
      Yield.None(Signal.Success, Validated.Valid)
  }

  it should "return Yield.Some if the input is Validated.Valid" in {
    val value = mock[AnyRef]
    Validated.Valid(cats.data.Validated.Valid(value)) shouldBe Yield.Some(value, Signal.Success, Validated.Valid)
  }
}
