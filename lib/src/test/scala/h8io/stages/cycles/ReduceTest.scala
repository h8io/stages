package h8io.stages.cycles

import h8io.stages.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReduceTest extends AnyFlatSpec with Matchers with Inside with MockFactory with StagesCoreTestUtil {
  "Reduce" should "yield a single output without applying op and absorb an error-free Complete" in {
    val in = 42L
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val op = mock[Stage[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution[(String, String), String, String]]("op evolution")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val nextOp = mock[Stage[(String, String), String, String]]("next op")
    inSequence {
      (alterand.apply _).expects(in).returns(Yield.Some("a", Status.complete, alterandEvolution))
      (op.skip _).expects().returns(opEvolution)
      (opEvolution.evolve _).expects(Status.complete).returns(nextOp)
      (alterandEvolution.evolve _).expects(Status.complete).returns(nextAlterand)
      inside(Reduce(alterand, op)(in)) { case Yield.Some(out, Status.Success, evolution) =>
        out shouldBe "a"
        testConstEvolution(evolution, Reduce(nextAlterand, nextOp))
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
    val op1 = mock[Stage[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution[(String, String), String, String]]("op evolution 2")
    val op3 = mock[Stage[(String, String), String, String]]("op 3")
    val opEvolution3 = mock[Evolution[(String, String), String, String]]("op evolution 3")
    val nextOp = mock[Stage[(String, String), String, String]]("next op")
    inSequence {
      (alterand1.apply _).expects(in).returns(Yield.Some("a", Status.Success, alterandEvolution1))
      (op1.skip _).expects().returns(opEvolution1)
      (opEvolution1.evolve _).expects(Status.Success).returns(op2)
      (alterandEvolution1.evolve _).expects(Status.Success).returns(alterand2)
      (alterand2.apply _).expects(in).returns(Yield.Some("b", Status.Success, alterandEvolution2))
      (op2.apply _).expects(("a", "b")).returns(Yield.Some("ab", Status.Success, opEvolution2))
      (opEvolution2.evolve _).expects(Status.Success).returns(op3)
      (alterandEvolution2.evolve _).expects(Status.Success).returns(alterand3)
      (alterand3.apply _).expects(in).returns(Yield.Some("c", complete, alterandEvolution3))
      (op3.apply _).expects(("ab", "c")).returns(Yield.Some("abc", Status.Success, opEvolution3))
      (opEvolution3.evolve _).expects(complete).returns(nextOp)
      (alterandEvolution3.evolve _).expects(complete).returns(nextAlterand)
      inside(Reduce(alterand1, op1)(in)) { case Yield.Some(out, status, evolution) =>
        out shouldBe "abc"
        status shouldBe complete
        testConstEvolution(evolution, Reduce(nextAlterand, nextOp))
        (opEvolution3.dispose _).expects()
        (alterandEvolution3.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "combine the statuses of an iteration in pipeline order: alterand first, then op" in {
    val in = 2L
    val alterand1 = mock[Stage[Long, String, String]]("alterand 1")
    val alterandEvolution1 = mock[Evolution[Long, String, String]]("alterand evolution 1")
    val alterand2 = mock[Stage[Long, String, String]]("alterand 2")
    val alterandEvolution2 = mock[Evolution[Long, String, String]]("alterand evolution 2")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val op1 = mock[Stage[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution[(String, String), String, String]]("op evolution 2")
    val nextOp = mock[Stage[(String, String), String, String]]("next op")
    val combined = Status.error("alterand error", "op error")
    inSequence {
      (alterand1.apply _).expects(in).returns(Yield.Some("a", Status.Success, alterandEvolution1))
      (op1.skip _).expects().returns(opEvolution1)
      (opEvolution1.evolve _).expects(Status.Success).returns(op2)
      (alterandEvolution1.evolve _).expects(Status.Success).returns(alterand2)
      (alterand2.apply _).expects(in).returns(Yield.Some("b", Status.error("alterand error"), alterandEvolution2))
      (op2.apply _).expects(("a", "b")).returns(Yield.Some("ab", Status.error("op error"), opEvolution2))
      (opEvolution2.evolve _).expects(combined).returns(nextOp)
      (alterandEvolution2.evolve _).expects(combined).returns(nextAlterand)
      inside(Reduce(alterand1, op1)(in)) { case Yield.Some(out, status, evolution) =>
        out shouldBe "ab"
        status shouldBe combined
        testConstEvolution(evolution, Reduce(nextAlterand, nextOp))
        (opEvolution2.dispose _).expects()
        (alterandEvolution2.dispose _).expects()
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
    val op1 = mock[Stage[(String, String), String, String]]("op 1")
    val opEvolution1 = mock[Evolution[(String, String), String, String]]("op evolution 1")
    val op2 = mock[Stage[(String, String), String, String]]("op 2")
    val opEvolution2 = mock[Evolution[(String, String), String, String]]("op evolution 2")
    val op3 = mock[Stage[(String, String), String, String]]("op 3")
    val opEvolution3 = mock[Evolution[(String, String), String, String]]("op evolution 3")
    val nextOp = mock[Stage[(String, String), String, String]]("next op")
    inSequence {
      (alterand1.apply _).expects(in).returns(Yield.Some("a", Status.Success, alterandEvolution1))
      (op1.skip _).expects().returns(opEvolution1)
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
      inside(Reduce(alterand1, op1)(in)) { case Yield.Some(out, Status.Success, evolution) =>
        out shouldBe "a"
        testConstEvolution(evolution, Reduce(nextAlterand, nextOp))
        (opEvolution3.dispose _).expects()
        (alterandEvolution3.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "yield None when the cycle completes without producing any output" in {
    val in = 4L
    val complete = Status.error("failure")
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val op = mock[Stage[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution[(String, String), String, String]]("op evolution")
    val nextAlterand = mock[Stage[Long, String, String]]("next alterand")
    val nextOp = mock[Stage[(String, String), String, String]]("next op")
    inSequence {
      (alterand.apply _).expects(in).returns(Yield.None(complete, alterandEvolution))
      (op.skip _).expects().returns(opEvolution)
      (opEvolution.evolve _).expects(complete).returns(nextOp)
      (alterandEvolution.evolve _).expects(complete).returns(nextAlterand)
      inside(Reduce(alterand, op)(in)) { case Yield.None(status, evolution) =>
        status shouldBe complete
        testConstEvolution(evolution, Reduce(nextAlterand, nextOp))
        (opEvolution.dispose _).expects()
        (alterandEvolution.dispose _).expects()
        noException should be thrownBy evolution.dispose()
      }
    }
  }

  it should "skip both inner stages and evolve the skipped continuations on Success" in {
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val skippedAlterand = mock[Stage[Long, String, String]]("skipped alterand")
    val op = mock[Stage[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution[(String, String), String, String]]("op evolution")
    val skippedOp = mock[Stage[(String, String), String, String]]("skipped op")
    inSequence {
      (alterand.skip _).expects().returns(alterandEvolution)
      (op.skip _).expects().returns(opEvolution)
      (opEvolution.evolve _).expects(Status.Success).returns(skippedOp)
      (alterandEvolution.evolve _).expects(Status.Success).returns(skippedAlterand)
      val result = Reduce(alterand, op).skip()
      testConstEvolution(result, Reduce(skippedAlterand, skippedOp))
      (opEvolution.dispose _).expects()
      (alterandEvolution.dispose _).expects()
      noException should be thrownBy result.dispose()
    }
  }

  it should "dispose the alterand evolution and suppress its exception when both dispose calls throw" in {
    val alterand = mock[Stage[Long, String, String]]("alterand")
    val alterandEvolution = mock[Evolution[Long, String, String]]("alterand evolution")
    val skippedAlterand = mock[Stage[Long, String, String]]("skipped alterand")
    val op = mock[Stage[(String, String), String, String]]("op")
    val opEvolution = mock[Evolution[(String, String), String, String]]("op evolution")
    val skippedOp = mock[Stage[(String, String), String, String]]("skipped op")
    val primary = new RuntimeException("op dispose failure")
    val secondary = new RuntimeException("alterand dispose failure")
    inSequence {
      (alterand.skip _).expects().returns(alterandEvolution)
      (op.skip _).expects().returns(opEvolution)
      (opEvolution.evolve _).expects(Status.Success).returns(skippedOp)
      (alterandEvolution.evolve _).expects(Status.Success).returns(skippedAlterand)
      val result = Reduce(alterand, op).skip()
      (opEvolution.dispose _).expects().throws(primary)
      (alterandEvolution.dispose _).expects().throws(secondary)
      val thrown = the[RuntimeException] thrownBy result.dispose()
      thrown should be theSameInstanceAs primary
      thrown.getSuppressed should contain(secondary)
    }
  }
}
