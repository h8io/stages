package io.h8.stages.util

import io.h8.stages.State
import io.h8.stages.test.{AppendStage, CompleteStage, Counter, RedoStage, StageExtension}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SeqForkTest extends AnyFlatSpec with Matchers {
  classOf[SeqFork[?, ?, ?]].getSimpleName should "produce correct results" in {
    implicit val counter: Counter = new Counter
    (SeqFork(
      AppendStage("a").once ~> AppendStage("b").once,
      AppendStage("1").once ~> AppendStage("2").once ~> AppendStage("3").once
    ) ~> CompleteStage[(String, String)].once ~> counter.once).execute("x") match {
      case State.Yield(("xab", "x123"), _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "produce correct result with redo" in {
    implicit val counter: Counter = new Counter
    (SeqFork(
      AppendStage("a").repeat(6) ~> RedoStage(2, AppendStage("b")).repeat(6) ~> AppendStage("c").repeat(4),
      AppendStage("1").repeat(4) ~>
        RedoStage(1, AppendStage("2")).repeat(4) ~>
        RedoStage(2, AppendStage("3")).repeat(3) ~>
        AppendStage("4").once
    ) ~> CompleteStage[(String, String)].once ~> counter.once).execute("x") match {
      case State.Yield(("xabc", "x1234"), _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
