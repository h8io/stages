package h8io.stages.cats

import h8io.stages.base.StageOps
import h8io.stages.{Status, Yield}
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ValidatedTest extends AnyFlatSpec with Matchers with MockFactory {
  "Invalid" should "return Yield.Some if the input is Validated.Invalid" in {
    val value = mock[AnyRef]
    Validated.Invalid(cats.data.Validated.Invalid(value)) shouldBe
      Yield.Some(value, Status.Success, Validated.Invalid.toEvolution)
  }

  it should "return Yield.None if the input is Validated.Valid" in {
    Validated.Invalid[AnyRef].apply(cats.data.Validated.Valid(mock[AnyRef])) shouldBe
      Yield.None(Status.Success, Validated.Invalid.toEvolution)
  }

  "Valid" should "return Yield.None if the input is Validated.Invalid" in {
    Validated.Valid[AnyRef].apply(cats.data.Validated.Invalid(mock[AnyRef])) shouldBe
      Yield.None(Status.Success, Validated.Valid.toEvolution)
  }

  it should "return Yield.Some if the input is Validated.Valid" in {
    val value = mock[AnyRef]
    Validated.Valid(cats.data.Validated.Valid(value)) shouldBe
      Yield.Some(value, Status.Success, Validated.Valid.toEvolution)
  }
}
