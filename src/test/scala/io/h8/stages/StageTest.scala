package io.h8.stages

import io.h8.stages.util.{AppendStage, CompleteStage, RedoStage, RedoWhileStage}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageTest extends AnyFlatSpec with Matchers {
  classOf[Stage[?, ?]].getSimpleName should "produce the correct result" in {
    (AppendStage("a") ~> AppendStage("b") ~> AppendStage("c") ~> CompleteStage[String]).execute("x") match {
      case State.Yield("xabc", _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "redo the execution correctly" in {
    (AppendStage("a") ~>
      (RedoStage(1, AppendStage("1")) ~> AppendStage("b")) ~>
      (RedoStage(1, AppendStage("2")) ~> AppendStage("c")) ~>
      RedoStage(1, AppendStage("3")) ~>
      CompleteStage[String]).execute("x") match {
      case State.Yield("xa1b2c3", _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "loop the execution correctly" in {
    (AppendStage("a") ~> AppendStage("b") ~> AppendStage("c") ~> RedoWhileStage[String](_.length < 10))
      .recursion("x") match {
      case State.Yield("xabcabcabc", _, _) => succeed
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
