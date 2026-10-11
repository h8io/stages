package h8io.stages.cats

import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ValidatedTest extends AnyFlatSpec with Matchers with MockFactory {
  "Invalid" should "return Yield.Some if the input is Validated.Invalid" in {
    val value = new AnyRef
    Validated.Invalid[AnyRef](cats.data.Validated.Invalid(value)) shouldBe
      Yield.Some(value, Status.Success, Validated.Invalid)
  }

  it should "return Yield.None if the input is Validated.Valid" in {
    Validated.Invalid[AnyRef](cats.data.Validated.Valid(new AnyRef)) shouldBe
      Yield.None(Status.Success, Validated.Invalid)
  }

  "Valid" should "return Yield.None if the input is Validated.Invalid" in {
    Validated.Valid[AnyRef](cats.data.Validated.Invalid(new AnyRef)) shouldBe
      Yield.None(Status.Success, Validated.Valid)
  }

  it should "return Yield.Some if the input is Validated.Valid" in {
    val value = new AnyRef
    Validated.Valid[AnyRef](cats.data.Validated.Valid(value)) shouldBe
      Yield.Some(value, Status.Success, Validated.Valid)
  }
}
