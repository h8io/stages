package io.h8.stages

import io.h8.stages.test.{AppendStage, CompleteStage, Counter, RedoStage, RedoWhileStage}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageTest extends AnyFlatSpec with Matchers {
  classOf[Stage[?, ?]].getSimpleName should "produce the correct result" in {
    val counter = new Counter
    (counter.exactly(1) ~> AppendStage("a") ~>
      counter.exactly(1) ~> AppendStage("b") ~>
      counter.exactly(1) ~> AppendStage("c") ~>
      counter.exactly(1) ~> CompleteStage[String]).execute("x") match {
      case State.Yield("xabc", _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "redo the execution correctly" in {
    val counter = new Counter
    (counter.exactly(4) ~> AppendStage("a") ~>
      counter.exactly(4) ~> (RedoStage(1, AppendStage("1")) ~> AppendStage("b")) ~>
      counter.exactly(3) ~> (RedoStage(1, AppendStage("2")) ~> AppendStage("c")) ~>
      counter.exactly(2) ~> RedoStage(1, AppendStage("3")) ~>
      counter.exactly(1) ~> CompleteStage[String]).execute("x") match {
      case State.Yield("xa1b2c3", _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "loop the execution correctly" in {
    val counter = new Counter
    (counter.exactly(3) ~> AppendStage("a") ~>
      counter.exactly(3) ~> AppendStage("b") ~>
      counter.exactly(3) ~> AppendStage("c") ~>
      counter.exactly(3) ~> RedoWhileStage[String](_.length < 10))
      .recursion("x") match {
      case State.Yield("xabcabcabc", _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
