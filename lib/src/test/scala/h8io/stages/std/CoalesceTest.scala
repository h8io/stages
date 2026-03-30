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

  it should "provide a typed view via apply[T]" in {
    val stage = Coalesce[Int]
    stage(Left(1)) shouldBe Yield.Some(1, Status.Success, Coalesce)
    stage(Right(2)) shouldBe Yield.Some(2, Status.Success, Coalesce)
  }
}
