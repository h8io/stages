package h8io.stages.std

import h8io.stages.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScanTest extends AnyFlatSpec with Matchers with Inside with MockFactory {
  "Scan" should "become the accumulator on the first input, without applying op" in {
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    (op.skip _).expects().returns(opEvolution)
    inside(Scan(op)("a")) { case Yield.Some(out, Status.Success, _) =>
      out shouldBe "a"
    }
  }

  it should "fold each subsequent input into the running accumulator with op" in {
    val op1 = mock[Stage.Fruitful[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage.Fruitful[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 2")
    val op3 = mock[Stage.Fruitful[(String, String), String, String]]("op 3")
    val opEvolution3 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 3")

    inSequence {
      (op1.skip _).expects().returns(opEvolution1)
      inside(Scan(op1)("a")) { case Yield.Some("a", Status.Success, evolution1) =>
        (opEvolution1.evolve _).expects(Status.Success).returns(op2)
        val stage2 = evolution1.evolve(Status.Success)

        (op2.apply _).expects(("a", "b")).returns(Yield.Some.Fruitful("ab", Status.Success, opEvolution2))
        inside(stage2("b")) { case Yield.Some("ab", Status.Success, evolution2) =>
          (opEvolution2.evolve _).expects(Status.Success).returns(op3)
          val stage3 = evolution2.evolve(Status.Success)

          // The crux of the fix: op3 must fold against "ab" (the running total), not the stale seed "a".
          (op3.apply _).expects(("ab", "c")).returns(Yield.Some.Fruitful("abc", Status.Success, opEvolution3))
          inside(stage3("c")) { case Yield.Some(out, Status.Success, _) =>
            out shouldBe "abc"
          }
        }
      }
    }
  }

  it should "reset to a fresh Scan when a fold's status is Complete" in {
    val op1 = mock[Stage.Fruitful[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage.Fruitful[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 2")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    val nextOpEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("next op evolution")
    val complete = Status.error("done")

    inSequence {
      (op1.skip _).expects().returns(opEvolution1)
      inside(Scan(op1)("a")) { case Yield.Some("a", Status.Success, evolution1) =>
        (opEvolution1.evolve _).expects(Status.Success).returns(op2)
        val stage2 = evolution1.evolve(Status.Success)

        (op2.apply _).expects(("a", "b")).returns(Yield.Some.Fruitful("ab", complete, opEvolution2))
        inside(stage2("b")) { case Yield.Some("ab", status, evolution2) =>
          status shouldBe complete

          (opEvolution2.evolve _).expects(complete).returns(nextOp)
          val stage3 = evolution2.evolve(complete)

          // Complete resets: "c" seeds a fresh Scan instead of folding against the just-emitted "ab".
          (nextOp.skip _).expects().returns(nextOpEvolution)
          inside(stage3("c")) { case Yield.Some(out, Status.Success, _) =>
            out shouldBe "c"
          }
        }
      }
    }
  }

  it should "skip op via the seeded state and keep folding with the same accumulator" in {
    val op1 = mock[Stage.Fruitful[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage.Fruitful[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 2")
    val op3 = mock[Stage.Fruitful[(String, String), String, String]]("op 3")
    val opEvolution3 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 3")

    inSequence {
      (op1.skip _).expects().returns(opEvolution1)
      inside(Scan(op1)("a")) { case Yield.Some("a", Status.Success, evolution1) =>
        (opEvolution1.evolve _).expects(Status.Success).returns(op2)
        val stage2 = evolution1.evolve(Status.Success)

        (op2.skip _).expects().returns(opEvolution2)
        val skippedEvolution = stage2.skip()

        (opEvolution2.evolve _).expects(Status.Success).returns(op3)
        val stage3 = skippedEvolution.evolve(Status.Success)

        (op3.apply _).expects(("a", "c")).returns(Yield.Some.Fruitful("ac", Status.Success, opEvolution3))
        inside(stage3("c")) { case Yield.Some(out, Status.Success, _) =>
          out shouldBe "ac"
        }
      }
    }
  }

  it should "reset to a fresh Scan when the seeded state is skipped with a Complete status" in {
    val op1 = mock[Stage.Fruitful[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage.Fruitful[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 2")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    val nextOpEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("next op evolution")
    val complete = Status.error("done")

    inSequence {
      (op1.skip _).expects().returns(opEvolution1)
      inside(Scan(op1)("a")) { case Yield.Some("a", Status.Success, evolution1) =>
        (opEvolution1.evolve _).expects(Status.Success).returns(op2)
        val stage2 = evolution1.evolve(Status.Success)

        (op2.skip _).expects().returns(opEvolution2)
        val skippedEvolution = stage2.skip()

        (opEvolution2.evolve _).expects(complete).returns(nextOp)
        val stage3 = skippedEvolution.evolve(complete)

        (nextOp.skip _).expects().returns(nextOpEvolution)
        inside(stage3("c")) { case Yield.Some(out, Status.Success, _) =>
          out shouldBe "c"
        }
      }
    }
  }

  it should "skip op via the unseeded stage and evolve to a fresh Scan on the skipped continuation" in {
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    inSequence {
      (op.skip _).expects().returns(opEvolution)
      val evolution = Scan(op).skip()
      (opEvolution.evolve _).expects(Status.Success).returns(nextOp)
      evolution.evolve(Status.Success) shouldBe Scan(nextOp)
      (opEvolution.dispose _).expects()
      noException should be thrownBy evolution.dispose()
    }
  }

  it should "dispose delegate to the op evolution from the seeding step" in {
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    (op.skip _).expects().returns(opEvolution)
    inside(Scan(op)("a")) { case Yield.Some(_, _, evolution) =>
      (opEvolution.dispose _).expects()
      noException should be thrownBy evolution.dispose()
    }
  }
}
