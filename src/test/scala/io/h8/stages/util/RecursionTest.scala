package io.h8.stages.util

import io.h8.stages.State
import io.h8.stages.test.{AppendStage, Counter, RedoWhileStage, StageExtension}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RecursionTest extends AnyFlatSpec with Matchers {
  classOf[Recursion[?]].getSimpleName should "loop the execution correctly" in {
    implicit val counter: Counter = new Counter
    Recursion(AppendStage("a").repeat(3) ~>
      AppendStage("b").repeat(3) ~>
      AppendStage("c").repeat(3) ~>
      RedoWhileStage[String](_.length < 10).repeat(3) ~>
      counter.repeat(3))
      .execute("x") match {
      case State.Yield("xabcabcabc", _, _) => counter.validate()
      case unexpected => fail(s"Unexpected state $unexpected")
    }
  }
}
