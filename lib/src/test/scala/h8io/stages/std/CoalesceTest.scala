package h8io.stages.std

import h8io.stages.{Status, Yield}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant

class CoalesceTest extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {
  "Coalesce" should "return the left value when input is Left" in
    forAll { (value: String) =>
      Coalesce(Left(value)) shouldBe Yield.Some(value, Status.Success, Coalesce)
    }

  it should "return the right value when input is Right" in
    forAll { (value: Instant) =>
      Coalesce(Right(value)) shouldBe Yield.Some(value, Status.Success, Coalesce)
    }

  it should "be a polymorphic singleton" in {
    Coalesce[Long] should be theSameInstanceAs Coalesce
  }
}
