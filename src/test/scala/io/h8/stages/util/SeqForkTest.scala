package io.h8.stages.util

import io.h8.stages.State
import io.h8.stages.test.{AppendStage, CompleteStage, Counter}
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
}
