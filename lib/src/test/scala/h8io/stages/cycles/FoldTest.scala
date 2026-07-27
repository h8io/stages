package h8io.stages.cycles

import h8io.stages.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FoldTest extends AnyFlatSpec with Matchers with Inside with MockFactory with StagesCoreTestUtil {
  "Fold" should "fold the seed's first output through op and absorb an error-free Complete" in {
    val in = 5L
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    inSequence {
      (alterand.apply _).expects(in).returns(Yield.Some("a", Status.complete, alterandEvolution))
      (op.apply _).expects(("z", "a")).returns(Yield.Some.Fruitful("za", Status.Success, opEvolution))
      (opEvolution.evolve _).expects(Status.complete).returns(nextOp)
      (alterandEvolution.evolve _).expects(Status.complete).returns(nextAlterand)
      inside(Fold(alterand, op)(("z", in))) { case Yield.Some(out, Status.Success, evolution) =>
        out shouldBe "za"
        testConstEvolution(evolution, Fold(nextAlterand, nextOp))
        (opEvolution.dispose _).expects()
        (alterandEvolution.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "fold each subsequent output into the accumulator with op" in {
    val in = 1L
    val complete = Status.error("done")
    val alterand1 = mock[Stage[Long, String, String]]("alterand 1")
    val alterandEvolution1 = mock[Evolution[Long, String, String]]("alterand evolution 1")
    val alterand2 = mock[Stage[Long, String, String]]("alterand 2")
    val alterandEvolution2 = mock[Evolution[Long, String, String]]("alterand evolution 2")
    val alterand3 = mock[Stage[Long, String, String]]("alterand 3")
    val alterandEvolution3 = mock[Evolution[Long, String, String]]("alterand evolution 3")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val op1 = mock[Stage.Fruitful[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage.Fruitful[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 2")
    val op3 = mock[Stage.Fruitful[(String, String), String, String]]("op 3")
    val opEvolution3 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 3")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    inSequence {
      (alterand1.apply _).expects(in).returns(Yield.Some("a", Status.Success, alterandEvolution1))
      (op1.apply _).expects(("z", "a")).returns(Yield.Some.Fruitful("za", Status.Success, opEvolution1))
      (opEvolution1.evolve _).expects(Status.Success).returns(op2)
      (alterandEvolution1.evolve _).expects(Status.Success).returns(alterand2)
      (alterand2.apply _).expects(in).returns(Yield.Some("b", Status.Success, alterandEvolution2))
      (op2.apply _).expects(("za", "b")).returns(Yield.Some.Fruitful("zab", Status.Success, opEvolution2))
      (opEvolution2.evolve _).expects(Status.Success).returns(op3)
      (alterandEvolution2.evolve _).expects(Status.Success).returns(alterand3)
      (alterand3.apply _).expects(in).returns(Yield.Some("c", complete, alterandEvolution3))
      (op3.apply _).expects(("zab", "c")).returns(Yield.Some.Fruitful("zabc", Status.Success, opEvolution3))
      (opEvolution3.evolve _).expects(complete).returns(nextOp)
      (alterandEvolution3.evolve _).expects(complete).returns(nextAlterand)
      inside(Fold(alterand1, op1)(("z", in))) { case Yield.Some(out, status, evolution) =>
        out shouldBe "zabc"
        status shouldBe complete
        testConstEvolution(evolution, Fold(nextAlterand, nextOp))
        (opEvolution3.dispose _).expects()
        (alterandEvolution3.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "combine the statuses of an iteration in pipeline order: alterand first, then op" in {
    val in = 2L
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    val combined = Status.error("alterand error", "op error")
    inSequence {
      (alterand.apply _).expects(in).returns(Yield.Some("a", Status.error("alterand error"), alterandEvolution))
      (op.apply _).expects(("z", "a")).returns(Yield.Some.Fruitful("za", Status.error("op error"), opEvolution))
      (opEvolution.evolve _).expects(combined).returns(nextOp)
      (alterandEvolution.evolve _).expects(combined).returns(nextAlterand)
      inside(Fold(alterand, op)(("z", in))) { case Yield.Some(out, status, evolution) =>
        out shouldBe "za"
        status shouldBe combined
        testConstEvolution(evolution, Fold(nextAlterand, nextOp))
        (opEvolution.dispose _).expects()
        (alterandEvolution.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "keep the accumulator and skip op when the alterand yields no output" in {
    val in = 3L
    val alterand1 = mock[Stage[Long, String, String]]("alterand 1")
    val alterandEvolution1 = mock[Evolution[Long, String, String]]("alterand evolution 1")
    val alterand2 = mock[Stage[Long, String, String]]("alterand 2")
    val alterandEvolution2 = mock[Evolution[Long, String, String]]("alterand evolution 2")
    val alterand3 = mock[Stage[Long, String, String]]("alterand 3")
    val alterandEvolution3 = mock[Evolution[Long, String, String]]("alterand evolution 3")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val op1 = mock[Stage.Fruitful[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage.Fruitful[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 2")
    val op3 = mock[Stage.Fruitful[(String, String), String, String]]("op 3")
    val opEvolution3 = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution 3")
    val nextOp = mock[Stage.Fruitful[(String, String), String, String]]("next op")
    inSequence {
      (alterand1.apply _).expects(in).returns(Yield.Some("a", Status.Success, alterandEvolution1))
      (op1.apply _).expects(("z", "a")).returns(Yield.Some.Fruitful("za", Status.Success, opEvolution1))
      (opEvolution1.evolve _).expects(Status.Success).returns(op2)
      (alterandEvolution1.evolve _).expects(Status.Success).returns(alterand2)
      (alterand2.apply _).expects(in).returns(Yield.None(Status.Success, alterandEvolution2))
      (op2.skip _).expects().returns(opEvolution2)
      (opEvolution2.evolve _).expects(Status.Success).returns(op3)
      (alterandEvolution2.evolve _).expects(Status.Success).returns(alterand3)
      (alterand3.apply _).expects(in).returns(Yield.None(Status.complete, alterandEvolution3))
      (op3.skip _).expects().returns(opEvolution3)
      (opEvolution3.evolve _).expects(Status.complete).returns(nextOp)
      (alterandEvolution3.evolve _).expects(Status.complete).returns(nextAlterand)
      inside(Fold(alterand1, op1)(("z", in))) { case Yield.Some(out, Status.Success, evolution) =>
        out shouldBe "za"
        testConstEvolution(evolution, Fold(nextAlterand, nextOp))
        (opEvolution3.dispose _).expects()
        (alterandEvolution3.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "skip both inner stages and evolve the skipped continuations on Success" in {
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val skippedAlterand = mock[Stage[Long, String, String]]("skipped alterand")
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    val skippedOp = mock[Stage.Fruitful[(String, String), String, String]]("skipped op")
    inSequence {
      (alterand.skip _).expects().returns(alterandEvolution)
      (op.skip _).expects().returns(opEvolution)
      (opEvolution.evolve _).expects(Status.Success).returns(skippedOp)
      (alterandEvolution.evolve _).expects(Status.Success).returns(skippedAlterand)
      val result = Fold(alterand, op).skip()
      testConstEvolution(result, Fold(skippedAlterand, skippedOp))
      (opEvolution.dispose _).expects()
      (alterandEvolution.dispose _).expects()
      noException should be thrownBy result.dispose()
    }
  }

  it should "dispose the alterand evolution and suppress its exception when both dispose calls throw" in {
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val skippedAlterand = mock[Stage[Long, String, String]]("skipped alterand")
    val op = mock[Stage.Fruitful[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution.Fruitful[(String, String), String, String]]("op evolution")
    val skippedOp = mock[Stage.Fruitful[(String, String), String, String]]("skipped op")
    val primary = new RuntimeException("op dispose failure")
    val secondary = new RuntimeException("alterand dispose failure")
    inSequence {
      (alterand.skip _).expects().returns(alterandEvolution)
      (op.skip _).expects().returns(opEvolution)
      (opEvolution.evolve _).expects(Status.Success).returns(skippedOp)
      (alterandEvolution.evolve _).expects(Status.Success).returns(skippedAlterand)
      val result = Fold(alterand, op).skip()
      (opEvolution.dispose _).expects().throws(primary)
      (alterandEvolution.dispose _).expects().throws(secondary)
      val thrown = the[RuntimeException] thrownBy result.dispose()
      thrown should be theSameInstanceAs primary
      thrown.getSuppressed should contain(secondary)
    }
  }
}
