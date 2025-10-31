package h8io.xi.stages.examples

import h8io.xi.stages.{Signal, Yield}
import org.scalacheck.Gen
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FactorialTest extends AnyFlatSpec with Matchers with Inside with ScalaCheckPropertyChecks {
  def factorial(n: Int): BigInt = (1 to n foldLeft One)(_ * _)

  "Factorial1" should "calculate factorial for positive arguments" in
    forAll(Gen.choose(1, 1000)) { n =>
      inside(Factorial1.stage(n)(())) { case Yield.Some(out, Signal.Success, _) => out shouldBe factorial(n) }
    }

  it should "calculate factorial for one" in {
    Factorial1.stage(1)(()) should matchPattern { case Yield.Some(One, Signal.Success, _) => }
  }

  it should "not calculate factorial for zero" in {
    Factorial1.stage(0)(()) should matchPattern { case Yield.None(Signal.Success, _) => }
  }

  it should "not calculate factorial for negative arguments" in
    forAll(Gen.choose(Int.MinValue, -1)) { n =>
      Factorial1.stage(n)(()) should matchPattern { case Yield.None(Signal.Success, _) => }
    }

  "Factorial1.Agg" should "return Agg object for onError method" in {
    Factorial1.Agg(42).onError() shouldBe Factorial1.Agg
  }

  "Factorial2" should "calculate factorial for positive arguments" in
    forAll(Gen.choose(1, 1000)) { n =>
      inside(Factorial2.stage(n)) { case Yield.Some(out, Signal.Success, _) => out shouldBe factorial(n) }
    }

  it should "calculate factorial for one" in {
    Factorial2.stage(1) should matchPattern { case Yield.Some(One, Signal.Success, _) => }
  }

  it should "calculate factorial for zero" in {
    Factorial2.stage(0) should matchPattern { case Yield.Some(One, Signal.Success, _) => }
  }

  it should "not calculate factorial for negative arguments" in {
    val expectedError = Signal.Error("negative number")
    forAll(Gen.choose(Int.MinValue, -1)) { n =>
      Factorial2.stage(n) should matchPattern { case Yield.None(`expectedError`, _) => }
    }
  }

  "Factorial3" should "calculate factorial for positive arguments" in {
    inside(Factorial3.stage(5)) { case Yield.Some(out, Signal.Success, _) => out shouldBe factorial(5) }
    forAll(Gen.choose(1, 1000)) { n =>
      inside(Factorial3.stage(n)) { case Yield.Some(out, Signal.Success, _) => out shouldBe factorial(n) }
    }
  }

  it should "calculate factorial for one" in {
    Factorial3.stage(1) should matchPattern { case Yield.Some(One, Signal.Success, _) => }
  }

  it should "calculate factorial for zero" in {
    Factorial3.stage(0) should matchPattern { case Yield.Some(One, Signal.Success, _) => }
  }

  it should "not calculate factorial for negative arguments" in {
    val expectedError = Signal.Error(Factorial3.NegativeNumberError)
    forAll(Gen.choose(Int.MinValue, -1)) { n =>
      Factorial3.stage(n) should matchPattern { case Yield.None(`expectedError`, _) => }
    }
  }

  it should "return the initial stage on complete" in
    forAll((i: Int, f: BigInt) => Factorial3.Factorial(i, f).onComplete() shouldBe Factorial3.InitialStage)

  it should "return the initial stage on error" in
    forAll((i: Int, f: BigInt) => Factorial3.Factorial(i, f).onError() shouldBe Factorial3.InitialStage)
}
