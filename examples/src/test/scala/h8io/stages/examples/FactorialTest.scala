package h8io.stages.examples

import h8io.stages.{Outcome, Status, Yield}
import org.scalacheck.Gen
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class FactorialTest extends AnyFlatSpec with Matchers with Inside with ScalaCheckPropertyChecks {
  def factorial(n: Int): BigInt = (1 to n foldLeft One)(_ * _)

  "Factorial1" should "calculate factorial for positive arguments" in
    forAll(Gen.choose(1, 1000)) { n =>
      inside(Factorial1.pipeline(n).execute(())) { case Outcome.Some(out, Status.Success, None) =>
        out shouldBe factorial(n)
      }
    }

  it should "calculate factorial for one" in {
    Factorial1.pipeline(1).execute(()) should matchPattern { case Outcome.Some(One, Status.Success, None) => }
  }

  it should "not calculate factorial for zero" in {
    Factorial1.pipeline(0).execute(()) should matchPattern { case Outcome.None(Status.Success, None) => }
  }

  it should "not calculate factorial for negative arguments" in
    forAll(Gen.choose(Int.MinValue, -1)) { n =>
      Factorial1.pipeline(n).execute(()) should matchPattern { case Outcome.None(Status.Success, None) => }
    }

  "Factorial2" should "calculate factorial for positive arguments" in
    forAll(Gen.choose(1, 1000)) { n =>
      inside(Factorial2.pipeline.execute(n)) { case Outcome.Some(out, Status.Success, None) =>
        out shouldBe factorial(n)
      }
    }

  it should "calculate factorial for one" in {
    Factorial2.pipeline.execute(1) should matchPattern { case Outcome.Some(One, Status.Success, None) => }
  }

  it should "calculate factorial for zero" in {
    Factorial2.pipeline.execute(0) should matchPattern { case Outcome.Some(One, Status.Success, None) => }
  }

  it should "not calculate factorial for negative arguments" in {
    val expectedError = Status.error("negative number")
    forAll(Gen.choose(Int.MinValue, -1)) { n =>
      Factorial2.pipeline.execute(n) should matchPattern { case Outcome.None(`expectedError`, None) => }
    }
  }

  "Factorial3" should "calculate factorial for positive arguments" in
    forAll(Gen.choose(1, 1000)) { n =>
      inside(Factorial3.pipeline.execute(n)) { case Outcome.Some(out, Status.Success, None) =>
        out shouldBe factorial(n)
      }
    }

  it should "calculate factorial for one" in {
    Factorial3.pipeline.execute(1) should matchPattern { case Outcome.Some(One, Status.Success, None) => }
  }

  it should "calculate factorial for zero" in {
    Factorial3.pipeline.execute(0) should matchPattern { case Outcome.Some(One, Status.Success, None) => }
  }

  it should "not calculate factorial for negative arguments" in {
    val expectedError = Status.error(Factorial3.NegativeNumberError)
    forAll(Gen.choose(Int.MinValue, -1)) { n =>
      Factorial3.pipeline.execute(n) should matchPattern { case Outcome.None(`expectedError`, None) => }
    }
  }

  // To cover some unused branches in Factorial3.Factorial
  "Factorial3 evolution" should "return the correct stage" in
    forAll(Gen.choose(1, 1000), Gen.choose(1, 1000)) { (n, m) =>
      val stage = Factorial3.Factorial(n, One)
      inside(stage(n + m)) { case Yield.Some(One, Status.Success, evolution) =>
        evolution.evolve(Status.Success) shouldBe Factorial3.Factorial(n + 1, BigInt(n))
        evolution.evolve(Status.complete) shouldBe Factorial3.InitialStage
        noException should be thrownBy evolution.dispose()
      }
    }
}
