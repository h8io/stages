package io.h8.stages

import io.h8.stages.test._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StageTest extends AnyFlatSpec with Matchers {
  classOf[Stage[?, ?]].getSimpleName should "produce the correct result" in {
    implicit val counter: Counter = new Counter
    (AppendStage("a").once ~>
      AppendStage("b").once ~>
      AppendStage("c").once ~>
      CompleteStage[String].once ~>
      counter.once).execute("x") match {
      case State.Yield("xabc", _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "redo the execution correctly" in {
    implicit val counter: Counter = new Counter
    (AppendStage("a").repeat(7) ~>
      (RedoStage(1, AppendStage("1")).repeat(7) ~> AppendStage("b").repeat(6)) ~>
      (RedoStage(2, AppendStage("2")).repeat(6) ~> AppendStage("c").repeat(4)) ~>
      RedoStage(3, AppendStage("3")).repeat(4) ~>
      CompleteStage[String].once ~>
      counter.once).execute("x") match {
      case State.Yield("xa1b2c3", _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }

  it should "done the execution correctly" in {
    implicit val counter: Counter = new Counter
    (AppendStage("a").once ~> DoneStage.once ~> AppendStage("b").never).execute("x") shouldBe a[State.Done[?, ?]]
  }
}
